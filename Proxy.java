package proxy;

import java.io.*; 
import java.net.*;
import java.util.*;
public class Proxy {

   
    private static HashMap<String, byte[]> cache = new HashMap<>();
    private static int proxyPort;
    private static String ServerHost;
    private static int ServerPort;

    public static void main(String[] args) {
        loadDonne();
        
        // mgereeer an azy tsirairay sy mandefa an le proxy
        new Thread(() -> startProxy()).start();

        // Gestion ana commadne
        startCommand();
    }


    private static void startProxy() {
        try (ServerSocket serverSocket = new ServerSocket(proxyPort)) {
            System.out.println("Proxy cache demarre sur le port " + proxyPort + "...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("Erreur lors du demarrage du serveur proxy : " + e.getMessage());
        }
    }

    private static void startCommand() {
        try (BufferedReader consoleSoratra = new BufferedReader(new InputStreamReader(System.in))) {
            String command;
            System.out.println("Entrez une commande :\n 1. 'clear-cache' pour vider le cache\n 2. 'exit' pour arreter le serveur\n 3. 'cache-status' pour afficher le contenu du cache \n et supprimer pour supprimer un cahe");

            while ((command = consoleSoratra.readLine()) != null) {
                processCommand(command);
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de la lecture des commandes : " + e.getMessage());
        }
    }

    private static void DeleterUnCache(){
        Scanner scanner = new Scanner(System.in);
        System.out.print("Entrez l 'index a supprimer : ");
        int index = scanner.nextInt(); 
         int i = 0;
         int temp = 0 ;
        for (String key : cache.keySet()) {
            if(index == i){
                cache.remove(key) ;
                System.out.println("Element supprimes ") ;
                temp++;
                break ;
            }
             i++ ;
        }
        if(temp==0){
        System.out.println("Aucun index a trouve sur "+ index ) ;
        }

    }


    private static void processCommand(String command) {
        switch (command.trim().toLowerCase()) {
            case "clear-cache":
                cache.clear();
                System.out.println("Cache vide.");
                break;
            case "cache-status":
                System.out.println("Contenu du cache :");
                if (cache.isEmpty()) {
                    System.out.println("Le cache est vide.");
                } else {
                     int index = 0;
                     for (String key : cache.keySet()) {
                       System.out.println(index+"- " + key);
                       index++ ;
                        }

                    
                }
                break;
            case "supprimer":
                DeleterUnCache() ;
                break ;

            case "exit":
                System.out.println("Arret du serveur proxy...");
                System.exit(0);
                break;
            default:
                System.out.println("Commande inconnue. Veuillez reessayer.");
                break;
        }
    }

   
    private static void loadDonne() {
        Properties properties = new Properties();
        try (InputStream input = new FileInputStream("config.properties")) {
            properties.load(input);
            proxyPort = Integer.parseInt(properties.getProperty("proxy.port"));
            ServerHost = properties.getProperty("serveur.host");
            ServerPort = Integer.parseInt(properties.getProperty("serveur.port"));
            System.out.println("Configuration chargee : proxyPort=" + proxyPort + ", ServerHost=" + ServerHost + ", ServerPort=" + ServerPort);
        } catch (IOException | NumberFormatException e) {
            System.err.println("Erreur lors du chargement de la configuration : " + e.getMessage());
            proxyPort = 7777;
            ServerHost = "localhost";
            ServerPort = 8888;
        }
    }

    // Gestion Clients
    static class ClientHandler implements Runnable {
        private Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                OutputStream out = clientSocket.getOutputStream()
            ) {
                String requestLine = in.readLine();
                System.out.println("Requete recue : " + requestLine);

                if (requestLine != null && requestLine.startsWith("GET")) {
                    String url = requestLine.split(" ")[1];

                    if (cache.containsKey(url)) {
                        System.out.println("Reponse trouvee dans le cache.");
                        sendResponse(out, cache.get(url));
                    } else {
                        System.out.println("Requete envoyee au serveur ...");
                        byte[] response = getFromServer(url);
                        cache.put(url, response);
                        sendResponse(out, response);
                    }
                }
            } catch (IOException e) {
                System.err.println("Erreur lors du traitement de la requete : " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.err.println("Erreur lors de la fermeture du socket : " + e.getMessage());
                }
            }
        }

        private byte[] getFromServer(String lien) {

            String url =lien ;

            try (
                Socket ServerSocket = new Socket(ServerHost, ServerPort);
                BufferedWriter ServerOut = new BufferedWriter(new OutputStreamWriter(ServerSocket.getOutputStream())); //sortie
                InputStream ServerIn = ServerSocket.getInputStream() 
            ) {
                ServerOut.write("GET " + url + " HTTP/1.1\r\n");
                ServerOut.write("Host: " + ServerHost + "\r\n");
                ServerOut.write("Connection: close\r\n\r\n");
                ServerOut.flush();

                ByteArrayOutputStream responseBuffer = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = ServerIn.read(buffer)) != -1) {
                    responseBuffer.write(buffer, 0, bytesRead);
                }

                return responseBuffer.toByteArray();
            } catch (IOException e) {
                System.err.println("Erreur lors de la connexion à serveur : " + e.getMessage());
                return "HTTP/1.1 500 Internal Server Error\r\n\r\nErreur lors de la recuperation du fichier.".getBytes();
            }
        }

        private void sendResponse(OutputStream out, byte[] response) throws IOException {
            out.write(response);
            out.flush();
        }
    }
}
