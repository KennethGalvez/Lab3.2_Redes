package com.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.*;

import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackException;
import org.jxmpp.stringprep.XmppStringprepException;

class Node {
    private String name;
    private String emailAddress; // Add an email address field
    private Map<String, Integer> neighbors = new HashMap<>();

    public Node(String name, String emailAddress) {
        this.name = name;
        this.emailAddress = emailAddress;
    }

    public String getName() {
        return name;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void addNeighbor(String neighborName, int distance) {
        neighbors.put(neighborName, distance);
    }

    public Map<String, Integer> getNeighbors() {
        return neighbors;
    }
}

class Packet {
    private String type;
    private Map<String, String> headers;
    private String payload;

    public Packet(String type, Map<String, String> headers, String payload) {
        this.type = type;
        this.headers = headers;
        this.payload = payload;
    }

    public String toJson() {
        return "{\n" +
                "\"type\": \"" + type + "\",\n" +
                "\"headers\": " + headers.toString() + ",\n" +
                "\"payload\": \"" + payload + "\"\n" +
                "}";
    }
}

class LinkStateRouting {
    private Map<String, Node> network = new HashMap<>();
    private Map<String, Map<String, Integer>> shortestPaths = new HashMap<>();

    public void addNode(String nodeName, String emailAddress) {
        Node node = new Node(nodeName, emailAddress);
        network.put(nodeName, node);
    }

    private ComunicacionXMPP xmpp;

    public LinkStateRouting(ComunicacionXMPP xmpp) {
        this.xmpp = xmpp;
    }

    public boolean isValidNode(String nodeName) {
        return network.containsKey(nodeName);
    }

    public Set<String> getNodeNames() {
        return network.keySet();
    }

    public void computeShortestPaths() {
        for (Node node : network.values()) {
            Map<String, Integer> shortestPath = computeShortestPath(node);
            shortestPaths.put(node.getName(), shortestPath);
        }
    }

    public Node getNode(String nodeName) {
        return network.get(nodeName);
    }

    public void sendPacket(String sourceNode, String destinationNode, int hopCount, String packetType,
            String packetPayload) {

        Map<String, String> headers = new HashMap<>();
        headers.put("from", sourceNode);
        headers.put("to", destinationNode);
        headers.put("hop_count", Integer.toString(hopCount));

        Packet packet = new Packet(packetType, headers, packetPayload); // Use the provided packet type and payload

        System.out.println("Preparing to send packet from " + sourceNode + " to " + destinationNode);
        System.out.println(packet.toJson());
        System.out.println();

        // Check if the user is online before sending the message
        Node destination = getNode(destinationNode);

        String destinationEmailAddress = destination.getEmailAddress();
        if (xmpp.isUserOnline(destinationEmailAddress)) {
            try {
                xmpp.iniciarChat(destinationEmailAddress);
                xmpp.enviarMensaje(packet.toJson(), packetPayload); // Pass both recipient and message
                // System.out.println("Packet sent successfully.");
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Failed to send message via XMPP.");
            }
        } else {
            System.out.println("User " + destinationNode + " is offline. Packet not sent.");

        }
    }

public void sendMessage(String sourceNode, String destinationNode, String message) throws XmppStringprepException {
    // Solicitar al usuario el tipo de paquete a enviar
    Scanner scanner = new Scanner(System.in);
    System.out.println("Seleccione el tipo de paquete a enviar:");
    System.out.println("1. Paquete ECHO");
    System.out.println("2. Paquete DATA");
    System.out.println("3. Paquete TABLE/INFO");
    System.out.print("Ingrese el número correspondiente al tipo de paquete: ");

    int packetTypeChoice = scanner.nextInt();
    scanner.nextLine(); // Consumir el salto de línea

    String packetType;
    String packetPayload;

    // Definir el tipo de paquete y su payload según la elección del usuario
    switch (packetTypeChoice) {
        case 1:
            packetType = "ECHO";
            long startTime = System.currentTimeMillis();
            int delay = calculateDelayBetweenNodes(sourceNode, destinationNode);
            packetPayload = "ECHO_DELAY: " + delay + " ms";
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;
            System.out.println("Tiempo de ejecución (ms): " + executionTime);
            break;
        case 2:
            packetType = "DATA";
            packetPayload = "DATA_MESSAGE: " + message;
            break;
        case 3:
            packetType = "TABLE/INFO";
            String routingTable = getRoutingTableAsString(sourceNode);
            packetPayload = "ROUTING_TABLE:\n" + routingTable;
            System.out.println("Tabla de enrutamiento:\n" + routingTable);
            break;
        default:
            System.out.println("Opción no válida. Se enviará un paquete DATA por defecto.");
            packetType = "DATA";
            packetPayload = "DATA_MESSAGE: " + message;
            break;
    }

    // Solicitar al usuario el método de enrutamiento
    System.out.println("Seleccione el método de enrutamiento:");
    System.out.println("1. Link State Routing");
    System.out.println("2. Flooding");
    System.out.print("Ingrese el número correspondiente al método de enrutamiento: ");

    int routingChoice = scanner.nextInt();
    scanner.nextLine(); // Consumir el salto de línea

    if (routingChoice == 1) {
        // Utilizar Link State Routing
        String shortestPath = findShortestPath(sourceNode, destinationNode);

        if (shortestPath.equals("No path found.")) {
            System.out.println("No se encontró un camino para enviar el mensaje.");
        } else {
            String[] pathNodes = shortestPath.split(" -> ");
            int hopCount = 1;

            System.out.println("\nEnviando mensaje desde " + sourceNode + " a " + destinationNode
                    + " a través del camino: " + shortestPath);

            for (int i = 0; i < pathNodes.length - 1; i++) {
                String currentNode = pathNodes[i];
                String nextNode = pathNodes[i + 1];

                // Enviar el paquete con el tipo y payload correspondientes
                sendPacket(currentNode, nextNode, hopCount, packetType, packetPayload);
                hopCount++;
            }
        }
    } else if (routingChoice == 2) {
        // Utilizar Flooding
        sendPacketToAllNodes(sourceNode, packetType, packetPayload);
    } else {
        System.out.println("Opción no válida. No se enviará ningún mensaje.");
    }
}

    private void sendPacketToAllNodes(String sourceNode, String packetType, String packetPayload) {
        System.out.println("\nEnviando mensaje desde " + sourceNode + " a todos los nodos de la topología:");

        for (String destination : getNodeNames()) {
            if (!destination.equals(sourceNode)) {
                // Enviar el paquete con el tipo y payload correspondientes a todos los nodos excepto al origen
                sendPacket(sourceNode, destination, 1, packetType, packetPayload);
            }
        }
    }

    // Función para calcular el delay entre nodos (simulación)
    private int calculateDelayBetweenNodes(String sourceNode, String destinationNode) throws XmppStringprepException {
        // Obtén el nodo de destino
        Node destination = getNode(destinationNode);

        if (destination == null) {
            return -1; // Nodo de destino no encontrado
        }

        try {
            // Inicia un chat con el nodo de destino
            xmpp.iniciarChat(destination.getEmailAddress());

            // Envía un mensaje vacío para medir el tiempo de respuesta
            String message = ""; // Mensaje vacío
            long startTime = System.currentTimeMillis();
            xmpp.enviarMensaje(message, destination.getEmailAddress());
            long endTime = System.currentTimeMillis();

            // Cierra el chat después de enviar el mensaje
            xmpp.cerrarChat();

            return (int) (endTime - startTime); // Devuelve el tiempo transcurrido como ping
        } catch (SmackException.NotConnectedException | InterruptedException e) {
            e.printStackTrace();
            return -1; // Error al calcular el ping
        }
    }

    // Función para obtener la tabla de enrutamiento como cadena de texto
    // (simulación)
    private String getRoutingTableAsString(String sourceNode) {
        // Obtener el nodo fuente
        Node source = getNode(sourceNode);

        if (source == null) {
            return "Nodo fuente no encontrado.";
        }

        // Inicializar una cadena de texto para la tabla de enrutamiento
        StringBuilder routingTable = new StringBuilder();
        routingTable.append("Destino\t\t\tPróximo Salto\t\tDistancia\n");

        for (Map.Entry<String, Integer> entry : source.getNeighbors().entrySet()) {
            String neighborName = entry.getKey();
            int distance = entry.getValue();
            routingTable.append(" " + sourceNode + "\t\t\t" + neighborName + "\t\t\t" + distance + "\n");
        }

        return routingTable.toString();
    }

    private Map<String, Integer> computeShortestPath(Node sourceNode) {
        PriorityQueue<NodeDistance> pq = new PriorityQueue<>();
        pq.offer(new NodeDistance(sourceNode, 0));

        Map<String, Integer> distanceMap = new HashMap<>();
        distanceMap.put(sourceNode.getName(), 0);

        while (!pq.isEmpty()) {
            NodeDistance nd = pq.poll();
            Node currentNode = nd.getNode();
            int currentDistance = nd.getDistance();

            for (Map.Entry<String, Integer> neighborEntry : currentNode.getNeighbors().entrySet()) {
                String neighborName = neighborEntry.getKey();
                int neighborDistance = neighborEntry.getValue();
                int newDistance = currentDistance + neighborDistance;

                if (!distanceMap.containsKey(neighborName) || newDistance < distanceMap.get(neighborName)) {
                    distanceMap.put(neighborName, newDistance);
                    pq.offer(new NodeDistance(network.get(neighborName), newDistance));
                }
            }
        }

        return distanceMap;
    }

    public void printRoutingTable() {
        for (Node node : network.values()) {
            System.out.println("\nRouting table for Node " + node.getName() + ":");
            for (String neighborName : node.getNeighbors().keySet()) {
                System.out.println(" Node " + node.getName() + " to " + neighborName +
                        ", Distance: " + shortestPaths.get(node.getName()).get(neighborName));
            }
            System.out.println();
        }
    }

    public String findShortestPath(String fromNode, String toNode) {
        if (!shortestPaths.containsKey(fromNode) || !shortestPaths.get(fromNode).containsKey(toNode)) {
            return "No path found.";
        }

        List<String> path = new ArrayList<>();
        String currentNode = toNode;

        while (!currentNode.equals(fromNode)) {
            path.add(currentNode);
            currentNode = getPreviousNodeInPath(fromNode, currentNode);
        }

        path.add(fromNode);
        Collections.reverse(path);

        return String.join(" -> ", path);
    }

    private String getPreviousNodeInPath(String sourceNode, String currentNode) {
        int shortestDistance = shortestPaths.get(sourceNode).get(currentNode);
        String previousNode = null;

        for (Map.Entry<String, Integer> entry : shortestPaths.get(sourceNode).entrySet()) {
            if (network.get(currentNode).getNeighbors().containsKey(entry.getKey()) && entry.getValue()
                    + network.get(currentNode).getNeighbors().get(entry.getKey()) == shortestDistance) {
                previousNode = entry.getKey();
                break;
            }
        }
        return previousNode;
    }

    private static class NodeDistance implements Comparable<NodeDistance> {
        private Node node;
        private int distance;

        public NodeDistance(Node node, int distance) {
            this.node = node;
            this.distance = distance;
        }

        public Node getNode() {
            return node;
        }

        public int getDistance() {
            return distance;
        }

        @Override
        public int compareTo(NodeDistance other) {
            return Integer.compare(this.distance, other.distance);
        }
    }
}

public class LinkStateRoutingMain {
    public static void main(String[] args) throws NotConnectedException, InterruptedException {
        Scanner scanner = new Scanner(System.in);

        System.out.print("XMPP Username: ");
        String username = scanner.nextLine();

        System.out.print("XMPP Password: ");
        String password = scanner.nextLine();

        ComunicacionXMPP xmpp = null;

        try {
            xmpp = new ComunicacionXMPP("alumchat.xyz", 5222, "alumchat.xyz");
            boolean loggedIn = xmpp.iniciarSesion(username, password);

            if (!loggedIn) {
                System.out.println("Login failed. Exiting.");
                return;
            }

            // Read the username-to-node mapping from names1-x-randomX-2023.json
            String sourceNode = getUsernameMapping(username);
            if (sourceNode == null) {
                System.out.println("Invalid username. Exiting.");
                return;
            }

            LinkStateRouting router = new LinkStateRouting(xmpp); // Initialize with the ComunicacionXMPP instance

            try {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode nodeData = objectMapper.readTree(new File("names1-x-randomX-2023.json"));
                JsonNode configNode = nodeData.get("config");

                Iterator<Map.Entry<String, JsonNode>> fields = configNode.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    String nodeName = entry.getKey();
                    String email = entry.getValue().asText();
                    router.addNode(nodeName, email); // Modify to accept email
                }

                // Read topology information from JSON file
                JsonNode topoData = objectMapper.readTree(new File("topo1-x-randomX-2023.json"));
                JsonNode topoConfig = topoData.get("config");

                // Create topology based on JSON data
                Iterator<Map.Entry<String, JsonNode>> topoFields = topoConfig.fields();
                while (topoFields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = topoFields.next();
                    String nodeName = entry.getKey();
                    JsonNode neighbors = entry.getValue();
                    Node node = router.getNode(nodeName);

                    for (JsonNode neighbor : neighbors) {
                        String neighborName = neighbor.asText();
                        int distance = 1; // You can adjust this distance as needed
                        node.addNeighbor(neighborName, distance);
                    }
                }

                router.computeShortestPaths();

                // Menú después de iniciar sesión
                while (true) {
                    System.out.println("\nMenú de opciones:");
                    System.out.println("1. Mostrar nodos");
                    System.out.println("2. Imprimir tablas");
                    System.out.println("3. Enviar mensaje a nodo");
                    System.out.println("4. Salir");
                    System.out.print("Seleccione una opción: ");

                    int opcion = scanner.nextInt();
                    scanner.nextLine(); // Consumir el salto de línea

                    switch (opcion) {
                        case 1:
                            // Mostrar nodos
                            System.out.println("\nUploaded  nodes:");
                            for (String nodeName : router.getNodeNames()) {

                                Node node = router.getNode(nodeName);
                                System.out.println(nodeName + " (" + node.getEmailAddress() + ")");

                            }
                            break;
                        case 2:
                            // Imprimir tablas
                            router.printRoutingTable();
                            break;
                        case 3:
                            // Enviar mensaje a nodo
                            System.out.print("Ingrese el nombre del nodo de destino: ");
                            String destinationNode = scanner.nextLine();

                            if (!router.isValidNode(destinationNode)) {
                                System.out.println("Nodo de destino no válido.");
                            } else if (destinationNode.equals(sourceNode)) {
                                System.out.println("No puedes enviar un mensaje a ti mismo.");
                            } else {
                                System.out.print("Ingrese el mensaje a enviar: ");
                                String message = scanner.nextLine();
                                router.sendMessage(sourceNode, destinationNode, message);
                            }
                            break;
                        case 4:
                            // Salir del programa
                            return;
                        default:
                            System.out.println("Opción no válida. Por favor, seleccione una opción válida.");
                            break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (xmpp != null) {
                xmpp.cerrarConexion();
            }
            scanner.close();
        }
    }

    // Function to get the node name based on the entered username
    private static String getUsernameMapping(String enteredUsername) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode nodeData = objectMapper.readTree(new File("names1-x-randomX-2023.json"));
            JsonNode configNode = nodeData.get("config");

            Iterator<Map.Entry<String, JsonNode>> fields = configNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String nodeName = entry.getKey();
                String email = entry.getValue().asText();

                // Check if the email address corresponds to the entered username
                if (email.equals(enteredUsername + "@alumchat.xyz")) {
                    return nodeName;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null; // Username not found in the mapping
    }
}