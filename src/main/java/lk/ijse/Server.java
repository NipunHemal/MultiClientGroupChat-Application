package lk.ijse;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Server {
    private static final int PORT = 3000;
//    static Set<DataOutputStream> clients = new HashSet<>();
    static Map<DataOutputStream,String> clients = new HashMap<>();


    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT);) {
            System.out.println("Group Chat Server started on port " + PORT + "...");

            while (true) {
                Socket socket = serverSocket.accept();

                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                DataInputStream in = new DataInputStream(socket.getInputStream());

                String userName = in.readUTF();

                System.out.println(userName + " Client Connected");

                synchronized (clients) {
                    clients.put(out,userName);
                }

                new Thread(new ClientHandler(socket, out, in)).start();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void broadcast(String message, byte[] imageBytes, DataOutputStream dosSender) {
        System.out.println("Broadcast to clients : "+message);
        synchronized (clients) {
            for (DataOutputStream dos : clients.keySet()) {
                if (dos == dosSender) continue;

                System.out.println(clients.get(dos));
                try {
                    if (message != null) {
                        dos.writeUTF(message);
                    } else if (imageBytes != null) {
                        dos.writeUTF("IMAGE");
                        dos.writeInt(imageBytes.length);
                        dos.write(imageBytes);
                    }
                    dos.flush();
                } catch (IOException e) {
                    clients.remove(dosSender);
                    System.out.println(clients.get(dosSender) + " is Disconnected");
                }
            }
        }
    }

    static class ClientHandler implements Runnable {
        Socket socket;
        DataInputStream in;
        DataOutputStream out;

        ClientHandler(Socket socket, DataOutputStream out, DataInputStream in) {
            this.socket = socket;
            this.out = out;
            this.in = in;
        }


        @Override
        public void run() {
            try {
                while (true) {
                    String message = in.readUTF();

                    if (message.equals("IMAGE")) {
                        int size = in.readInt();
                        byte[] imageBytes = new byte[size];
                        in.readFully(imageBytes);

                        System.out.println(clients.get(out)+" : Image received");
                        Server.broadcast(null, imageBytes, out);
                    } else {
                        System.out.println(clients.get(out)+" : Received Message : " + message);
                        Server.broadcast(message, null, out);
                    }
                }
            } catch (Exception e) {
                System.out.println(clients.get(out)+" is disconnected: " + socket.getInetAddress());
            } finally {
                try {
                    in.close();
                    in.close();
                    socket.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                synchronized (clients) {
                    clients.remove(out);
                }
            }
        }
    }
}
