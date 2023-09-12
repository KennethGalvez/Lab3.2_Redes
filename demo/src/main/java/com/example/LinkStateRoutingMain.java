package com.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.*;

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

    public void sendPacket(String sourceNode, String destinationNode, int hopCount, String payload) {
        Map<String, String> headers = new HashMap<>();
        headers.put("from", sourceNode);
        headers.put("to", destinationNode);
        headers.put("hop_count", Integer.toString(hopCount));

        Packet packet = new Packet("message", headers, payload);

        System.out.println("Sending packet from " + sourceNode + " to " + destinationNode);
        System.out.println(packet.toJson());
        System.out.println();
    }

    public void sendMessage(String sourceNode, String destinationNode, String message) {
    String shortestPath = findShortestPath(sourceNode, destinationNode);

    if (shortestPath.equals("No path found.")) {
        System.out.println("No path found for sending the message.");
    } else {
        String[] pathNodes = shortestPath.split(" -> ");
        int hopCount = 1;
        
        System.out.println("Sending message from " + sourceNode + " to " + destinationNode + " through path: " + shortestPath);

        for (int i = 0; i < pathNodes.length - 1; i++) {
            String currentNode = pathNodes[i];
            String nextNode = pathNodes[i + 1];
            
            sendPacket(currentNode, nextNode, hopCount, message);
            hopCount++;
        }
    }
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
            System.out.println("Routing table for Node " + node.getName() + ":");
            for (String neighborName : node.getNeighbors().keySet()) {
                System.out.println("To Node " + neighborName + " via " + neighborName +
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
            if (network.get(currentNode).getNeighbors().containsKey(entry.getKey()) && entry.getValue() + network.get(currentNode).getNeighbors().get(entry.getKey()) == shortestDistance) {
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
    public static void main(String[] args) {
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

            LinkStateRouting router = new LinkStateRouting();

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
                router.printRoutingTable();

                // Display available destination nodes excluding the source node (logged-in user)
                System.out.println("Available destination nodes:");
                for (String nodeName : router.getNodeNames()) {
                    if (!nodeName.equals(sourceNode)) {
                        Node node = router.getNode(nodeName);
                        System.out.println(nodeName + " (" + node.getEmailAddress() + ")");
                    }
                }
                System.out.print("Enter destination node: ");
                String destinationNode = scanner.nextLine();

                if (!router.isValidNode(destinationNode)) {
                    System.out.println("Invalid destination node. Exiting.");
                    return;
                }

                // Check if the destination node is the same as the source node (logged-in user)
                if (destinationNode.equals(sourceNode)) {
                    System.out.println("Cannot send a message to yourself. Exiting.");
                    return;
                }

                String message = "Hola mundo";

                router.sendMessage(sourceNode, destinationNode, message);
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
