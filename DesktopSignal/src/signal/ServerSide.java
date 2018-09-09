package signal;
import java.net.*;
import java.util.ArrayList;
import java.io.*;

public class ServerSide {
  private static BufferedReader sin,stdin;
  private static PrintWriter sout;
  private static ArrayList<Socket> CONNECTIONS = new ArrayList<Socket>();
  public static void main(String[] args) throws IOException
  {

    try
    {
        ServerSocket serverSocket = new ServerSocket(4443);
        while (true)
        {
          Socket clientSocket = serverSocket.accept();
          CONNECTIONS.add(clientSocket);
          System.out.println("New socket connected from: " + clientSocket.getLocalAddress().getHostName());
          sin = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
          System.out.println("Sin initialized");
          stdin = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("stdout initialized");
          sout = new PrintWriter(clientSocket.getOutputStream(), true);
          System.out.println("sout initialized");

          while(true) {

            String input = sin.readLine();
            if (input == null)
            {
              CONNECTIONS.remove(0);
              System.out.println("Client Disconnected");
              break;
            }
            System.out.println("client: " + input);

            if (input.equals("1") )
            {
              sout.println(stdin.readLine());
            }
          }


      }
    }
    catch (IOException e)
    {
      System.out.println("Exception on port 4444");
      e.printStackTrace();
    }
  }

}
