import java.io.*;
import java.net.*;
import java.util.*;

public class Chat {
    static final int A = 5;
    static final int B = 8;
    static final int ALPHABETSIZE = 64;
    static final int ASCIISTART = 32;

    static public int getInverse(int a) {
        int a_inv = 0;
        int flag = 0;
        for(int i = 0; i < ALPHABETSIZE; i++) {
            flag = (a*i)%64;
            if(flag != 1) {
                continue;
            } else {
                a_inv = i;
            }
        }
        return a_inv;

    }

    static public String affineEncrypt(String message, int A, int B) {
        message = message.toLowerCase();
        String cipher = "";
        for(int i = 0; i < message.length(); i++) {
            if(message.charAt(i) != ' ') {
                cipher += (char) ((A*((int)message.charAt(i)-ASCIISTART)+B)%ALPHABETSIZE + ASCIISTART); //97 is ascii value of a
            } else {
                cipher+= message.charAt(i);
            }
        }
        return cipher;
    }
    static public String affineDecrypt(String cipher, int A, int B) {
        String message = "";
        int a_inv = getInverse(A);

        for(int i = 0; i < cipher.length(); i++) {
            if(cipher.charAt(i) != ' ') {
                message+= (char) (((a_inv*(((int)cipher.charAt(i)+ASCIISTART)-B))%ALPHABETSIZE) + ASCIISTART);
            } else {
                message+=cipher.charAt(i);
            }
        }
        return message;
    }
    static public boolean isValid(String input) {
        if(input.charAt(0) != '@') {
            return false;
        }
        int start = 0;
        while(input.charAt(start) != ':') {
            start++;
            if(start >= input.length()) {
                return false;
            }
        }
        if(input.charAt(start + 1) != ' ') {
            return false;
        }
        return true;
    }

    public static void startChat() {
        try {
            chatSession();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static void chatSession() {
        // A chat session starts by connecting to the server's TCP/IP socket.
        // We also create a new Scanner, to receive chat messages from the local user
        // in order to forward them to the server.
        //
        // By allocating these resources in a "try" statement, we ensure they
        // are released when control exits this procedure. See
        // https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html
        //
        try (
            final var inputFromUser = new Scanner(System.in);
            final var socket = new Socket("chat.bcs1110.svc.leastfixedpoint.nl", 5999);
        ) {
            // If control reaches here, the socket exists and is connected. We
            // extract an *input stream*, for reading messages from the server, and
            // an *output stream*, for sending messages to the server.
            final var inputFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            final var output = new PrintWriter(socket.getOutputStream());

            // Our chat system follows a very simple *communication protocol*.
            // https://en.wikipedia.org/wiki/Communication_protocol
            //
            // The first step in the protocol is for a connecting client to send
            // a "password" to access the server. It's not a big secret - it just
            // keeps opportunistic passers-by (e.g. AI crawler bots) out.
            output.println("F4EF9A36-5FCD-4D27-8A0A-FC7C77D3DBB2");
            output.flush();
            // Q. Why is this ^ `flush()` statement necessary?
            //    See https://stackoverflow.com/questions/2340106/what-is-the-purpose-of-flush-in-java-streams

            // After the password, the protocol proceeds *asynchronously*. The server
            // sends stuff to us when it has anything for us, and we send stuff to it
            // when we have anything to say. These can happen at any time. But in Java,
            // things happen one after the other! There's no notion of asynchronous
            // event! We *could* introduce threads or some other construct for representing
            // concurrent activity. (See https://en.wikipedia.org/wiki/Concurrency_(computer_science))
            //
            // But to keep it simple, THIS client program chooses to proceed in alternating
            // stages:
            //
            // 1 - "ping" the server, and print output we get back until we see the response to
            //     our "ping", indicating that there's nothing more to show just at the minute;
            // 2 - then, wait for input from the user.
            // 3 - send the message the user typed to the server, and loop back to the
            //     beginning again.
            //
            // We take special care not to send an empty line because that's what the protocol
            // uses to mean "ping"!
            //
            while (true) {
                // 1. Ping the server.
                output.println("");
                output.flush(); // Don't forget to actually send the output through!

                // 1(b). Collect responses until we see the one that specifically indicates
                // that the server has received and processed our "ping".
                while (true) {
                    var fromServer = inputFromServer.readLine();
                    if (fromServer == null) return; // We get null if the server disconnects.

                    if (fromServer.equals("+")) {
                        // Aha! The response to our ping! We're done with this round: move
                        // on to step 2.
                        break;
                    }
                    fromServer = fromServer.toUpperCase();
                    fromServer = affineDecrypt(fromServer, A, B);
                    // Some other message from the server: just print it.
                    System.out.println(fromServer);
                }

                // 2. Collect a line of input from the user.
                var messageToSend = inputFromUser.nextLine().toUpperCase().split(",");

                if(messageToSend.length >=5) {
                    System.out.println("too many arguments");
                    throw new IllegalArgumentException("too many arguments");
                }



                int keyA = A;
                int keyB = B;

                if(messageToSend.length == 4) {
                    keyB = Integer.parseInt(messageToSend[2]);
                    keyA = Integer.parseInt(messageToSend[1]);
                }

                int lastIndex = messageToSend.length - 1;
                if(!isValid(messageToSend[lastIndex])) {
                    System.out.println("please format your string correctly.");
                    messageToSend[lastIndex] = "";
                } else if (!messageToSend[lastIndex].equals("")) {
                    // 3. If it WASN'T a blank line, send it: it will be interpreted as
                    // a chat message.
                    String cipheredMessage = affineEncrypt(messageToSend[lastIndex], keyA, keyB);
                    output.println(cipheredMessage);

                    // Q. What would happen if we DIDN'T take care not to send empty lines?
                    //    Hint: what does our code do if it sees two "ping" responses ("+" lines) in a row?
                }

                // 3(b). Loop back to step 1.
            }
        } catch (Throwable t) {
            // We are being very lazy here and not handling errors properly.
            // In a real program, we would want to handle errors relating to network
            // failure differently from errors relating to, say, errors we made in our
            // own program being signalled, or errors relating to collecting input from
            // the user.
            t.printStackTrace();
        }
    }
}
