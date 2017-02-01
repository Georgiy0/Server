package edu.technopolis;

import javax.json.Json;
import java.net.*;
import java.io.*;
import javax.json.JsonObject;
import javax.json.JsonReader;


/**
 * Created by nsuprotivniy on 24.01.17.
 */
public class Server {

    Server(int port) {
        ServerSocket server = null;

        try {
            server = new ServerSocket(port);

            System.out.println("Waiting for a client...");
            while (true) {
                Socket client = server.accept();
                System.out.println("Client connected");
                new Thread( new SocketProcessor(client) ).start();
            }

        } catch (IOException e) {
            System.out.println("Couldn't listen to port " + port);
            System.out.println(e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                server.close();
            } catch (Exception e) {
                System.out.println("Can't close server");
                System.out.println(e.getMessage());
                e.printStackTrace();
            }


        }
    }

    private class SocketProcessor implements Runnable {

        Socket client;
        PostsHandler postsHandler;

        SocketProcessor(Socket client) {

            this.client = client;
            this.postsHandler = PostsHandler.getInstance();
        }

        public void run() {
            try {
                listenForClient();
            } catch (Throwable t) {
                System.out.println("Connection error");
                t.printStackTrace();

            } finally {
                try {
                    client.close();
                } catch (Throwable t) {
                    System.out.println("Can't close client");
                    t.printStackTrace();
                }
            }
        }

        private void listenForClient() throws Throwable {

            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                PrintWriter out = new PrintWriter(client.getOutputStream(), true);
                String request;

                while ((request = in.readLine()) != null) {
                    System.out.println(request);

                    //request = "{\"cmd\": \"find_post\",\"content\": {\"author\": \"Noname\"}}";

                    JsonObject result = handleRequest(request);

                    if (result == null) {
                        System.out.println("Connection disabled.");
                        break;
                    }

                    System.out.println(result.toString());
                    out.println(result.toString());

                }

            } catch (IOException e) {
                System.out.println("Stream error");
                System.out.println(e.getMessage());
                e.printStackTrace();
            } finally {
                client.close();
                postsHandler.close();
            }
        }

        private JsonObject handleRequest(String request) {
            JsonObject commandJSON;

            try {
                JsonReader reader =  Json.createReader(new StringReader(request));
                commandJSON = reader.readObject();
            } catch (Exception e) {
                System.out.println("Parse error");
                System.out.println(e.getMessage());
                e.printStackTrace();
                return Json.createObjectBuilder().add("result", "unsuccessful").build();
            }

            try {
                String command = commandJSON.getString("cmd");

                switch (command) {
                    case "exit": return null;
                    case "subscribe_posts": return subscribePosts();
                    case "find_posts": return postsHandler.find(commandJSON.getJsonObject("content"));
                    case "get_all_posts":  return postsHandler.getAll();
                    case "getLastPosts": return postsHandler.getLastPosts(commandJSON.getJsonObject("content"));
                    case "save_post": return postsHandler.save(commandJSON.getJsonObject("content"));
                    default: return Json.createObjectBuilder().add("result", "unsuccessful").build();
                }


            } catch (Exception e) {
                System.out.println("Error while reading json command");
                System.out.println(e.getMessage());
                e.printStackTrace();
                return Json.createObjectBuilder().add("result", "unsuccessful").build();
            }
        }

        private JsonObject subscribePosts() {
            try {
                PostsSubscriber subscriber = new PostsSubscriber(client);
                postsHandler.addSubscriber(subscriber);
                return JSONHandler.generateAnswer("subscribe_posts", Json.createObjectBuilder().build(), true);
            } catch (Exception e) {
                System.out.println("Can't create subscriber");
                System.out.println(e.getMessage());
                e.printStackTrace();
                return JSONHandler.generateAnswer("subscribe_posts", Json.createObjectBuilder().build(), false);
            }
        }
    }


    public static void main(String[] args) {
        Server server = new Server(8080);
    }
}
