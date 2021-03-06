package edu.utdallas.robotchess.pathplanning;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import edu.utdallas.robotchess.engine.Move;

public class MotionPlanner
{
    final int INF = 1000;
    final int REGULAR_SQUARE_COUNT = 64;
    final int REGULAR_ROW_SIZE = 8;
    final int REGULAR_COLUMN_SIZE = 8;
    private final static Logger log = Logger.getLogger(MotionPlanner.class);

    int boardRows;
    int boardColumns;

    public MotionPlanner(int boardRows, int boardColumns)
    {
        PropertyConfigurator.configure("log/log4j.properties");
        this.boardRows = boardRows;
        this.boardColumns = boardColumns;
    }

    //TODO: Should return an array of clear paths
    public Path checkIfPathsClear(int currentLocations[], Path paths[])
    {
        Path clearPath = null;
        boolean occupancyGrid[] = fillOccupancyGrid(currentLocations);

        log.info("Checking " + paths.length + " paths");

        for (Path path : paths) {
            ArrayList<Integer> squareSequence = path.getPath();
            log.info("Checking if path clear: " + path);
            boolean isPathClear = true;

            for (Integer square : squareSequence) {
                int squareLocation = square.intValue();
                isPathClear = !occupancyGrid[squareLocation];
                log.info("Square " + squareLocation + " unoccupied: " + isPathClear);

                if (!isPathClear) {
                    log.info("Path is not clear: " + path);
                    break;
                }
            }

            if (isPathClear) {
                clearPath = path;
                break;
            }
        }
        return clearPath;
    }

    public ArrayList<Path> plan(int currentLocations[], int desiredLocations[], Path desiredPaths[])
    {
        ArrayList<Path> plan = new ArrayList<>();
        Path clearPath = checkIfPathsClear(currentLocations, desiredPaths);

        log.info("Path in MotionPlanner: " + clearPath);

        //May want to modify plan() so that it clears the desired path
        if (clearPath == null)
            plan = plan(currentLocations, desiredLocations);
        else
            plan.add(clearPath);

        return plan;
    }

    public ArrayList<Path> plan(int currentLocations[], int desiredLocations[])
    {
        ArrayList<Path> plan = new ArrayList<>();
        boolean occupancyGrid[] = fillOccupancyGrid(currentLocations);
        ArrayList<Move> movesNeeded = generateMoves(currentLocations, desiredLocations);

        // for now, let's not handle any more than single move plans
        if (movesNeeded.size() > 1)
            return plan;

        for (Move move : movesNeeded) {
            Path path = new Path(move.pieceID, move.origin);

            ArrayList<Integer> squareSequence = dijkstra(occupancyGrid,
                                                         move.origin,
                                                         move.destination);

            path.setSquareSequence(squareSequence);
            plan.add(path);
        }

        return plan;
    }

    private boolean[] fillOccupancyGrid(int currentLocations[])
    {
        boolean occupancyGrid[] = new boolean[REGULAR_SQUARE_COUNT];

        for (int i = 0; i < occupancyGrid.length; i++)
            occupancyGrid[i] = false;

        for (int i = 0; i < currentLocations.length; i++)
            if (currentLocations[i] != -1)
                occupancyGrid[currentLocations[i]] = true;

        return occupancyGrid;
    }

    private ArrayList<Move> generateMoves(int currentLocations[], int desiredLocations[])
    {
        ArrayList<Move> moves = new ArrayList<>();

        for (int i = 0; i < currentLocations.length; i++)
            if (currentLocations[i] != desiredLocations[i])
                moves.add(new Move(i, currentLocations[i], desiredLocations[i]));

        return moves;
    }

    private ArrayList<Edge> computeEdges(boolean[] occupancyGrid, int vertex)
    {
        final int LATERAL_WEIGHT = 2;
        final int DIAGONAL_WEIGHT = 50;

        ArrayList<Edge> edges = new ArrayList<>();

        // check north neighbor
        if (vertex > REGULAR_ROW_SIZE)
            edges.add(new Edge(vertex, vertex - REGULAR_ROW_SIZE, LATERAL_WEIGHT));

        // check northeast neighbor
        if (vertex > REGULAR_ROW_SIZE &&
            vertex % REGULAR_ROW_SIZE < boardColumns - 1)
            edges.add(new Edge(vertex, vertex - REGULAR_ROW_SIZE + 1, DIAGONAL_WEIGHT));

        // check east neighbor
        if (vertex % REGULAR_ROW_SIZE < boardColumns - 1)
            edges.add(new Edge(vertex, vertex + 1, LATERAL_WEIGHT));

        // check southeast neighbor
        if (vertex < REGULAR_COLUMN_SIZE * (boardRows - 1) &&
            vertex % REGULAR_ROW_SIZE < boardColumns - 1)
            edges.add(new Edge(vertex, vertex + REGULAR_ROW_SIZE + 1, DIAGONAL_WEIGHT));

        // check south neighbor
        if (vertex < REGULAR_COLUMN_SIZE * (boardRows - 1))
            edges.add(new Edge(vertex, vertex + REGULAR_ROW_SIZE, LATERAL_WEIGHT));

        // check southwest neighbor
        if (vertex < REGULAR_COLUMN_SIZE * (boardRows - 1) &&
            vertex % REGULAR_ROW_SIZE != 0)
            edges.add(new Edge(vertex, vertex + REGULAR_ROW_SIZE - 1, DIAGONAL_WEIGHT));

        // check west neighbor
        if (vertex % REGULAR_ROW_SIZE != 0)
            edges.add(new Edge(vertex, vertex - 1, LATERAL_WEIGHT));

        // check northwest neighbor
        if (vertex > REGULAR_ROW_SIZE &&
            vertex % REGULAR_ROW_SIZE != 0)
            edges.add(new Edge(vertex, vertex - REGULAR_ROW_SIZE - 1, DIAGONAL_WEIGHT));

        // we can't move to a square that is occupied by another piece
        // remove edges whose destinations are occupied
        for (int i = 0; i < edges.size(); i++) {
            Edge testEdge = edges.get(i);

            if (occupancyGrid[testEdge.destination])
                edges.remove(i--);
        }

        return edges;
    }

    private ArrayList<Integer> dijkstra(boolean[] occupancyGrid,
                                        int origin, int destination)
    {
        Vertex vertices[] = new Vertex[REGULAR_SQUARE_COUNT];
        PriorityQueue<Vertex> queue = new PriorityQueue<>(boardRows * boardColumns,
                                                          new VertexComparator());

        enqueueVertices(vertices, queue, origin);
        updateDistances(occupancyGrid, vertices, queue);

        ArrayList<Integer> path = generatePath(vertices, destination);

        return path;
    }

    private void enqueueVertices(Vertex vertices[], PriorityQueue<Vertex> queue,
                                 int origin)
    {
        for (int i = 0; i < vertices.length; i++) {
            if (i == origin)
                vertices[i] = new Vertex(i, 0);
            else
                vertices[i] = new Vertex(i, INF);

            queue.add(vertices[i]);
        }
    }

    private void updateDistances(boolean[] occupancyGrid, Vertex vertices[],
                                 PriorityQueue<Vertex> queue)
    {
        while (queue.size() > 0) {
            Vertex u = queue.poll();

            ArrayList<Edge> edges = computeEdges(occupancyGrid, u.id);

            for (Edge e : edges) {
                Vertex v = vertices[e.destination];

                if (v.distance > u.distance + e.weight) {
                    queue.remove(v);
                    v.distance = u.distance + e.weight;
                    v.predecessor = u;
                    queue.add(v);
                }

            }
        }
    }

    private ArrayList<Integer> generatePath(Vertex vertices[], int destination)
    {
        Stack<Vertex> stack = new Stack<>();
        Vertex u = vertices[destination];

        while (u.predecessor != null) {
            stack.push(u);
            u = u.predecessor;
        }

        ArrayList<Integer> path = new ArrayList<>();

        while (!stack.empty()) {
            u = stack.pop();
            path.add(u.id);
        }

        return path;
    }

}

class Vertex
{
    int distance;
    int id;
    Vertex predecessor;

    Vertex(int id, int distance)
    {
        this.distance = distance;
        this.id = id;
        predecessor = null;
    }

    @Override
    public String toString()
    {
        String str = new String();
        str = String.format("Vertex ID: %d. Distance: %d.", id, distance);
        return str;
    }
}

class Edge
{
    int origin;
    int destination;
    int weight;

    Edge(int origin, int destination, int weight)
    {
        this.origin = origin;
        this.destination = destination;
        this.weight = weight;
    }
}

class VertexComparator implements Comparator<Vertex>
{
    @Override
    public int compare(Vertex x, Vertex y)
    {
        if (x.distance < y.distance)
            return -1;

        if (x.distance > y.distance)
            return 1;

        return 0;
    }
}
