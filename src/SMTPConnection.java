import java.net.*;
import java.io.*;
import java.util.*;

    /**
     * Open an SMTP connection to a mailserver and send one mail.g
     *
     */
    @SuppressWarnings("unused")
    public class SMTPConnection {
        /* The socket to the server */
        private Socket connection;

        /* Streams for reading and writing the socket */
        private BufferedReader fromServer;
        private DataOutputStream toServer;

        private static final int SMTP_PORT = 25;
        private static final String CRLF = "\r\n";

        /* Are we connected? Used in close() to determine what to do. */
        private boolean isConnected = false;

        /* Create an SMTPConnection object. Create the socket and the
           associated streams. Initialize SMTP connection. */
        public SMTPConnection(Envelope envelope) throws IOException {

            connection = new Socket(envelope.DestAddr,SMTP_PORT);
            fromServer = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            toServer =  new DataOutputStream(connection.getOutputStream());

		/* Read a line from server and check that the reply code is 220.
		   If not, throw an IOException. */

            String firstLine = fromServer.readLine();
            if(parseReply(firstLine)!= 220){
                throw new IOException("Connection Failed!");
            }else{
			/* SMTP handshake. We need the name of the local machine.
			   Send the appropriate SMTP handshake command. */
                String localhost = InetAddress.getLocalHost().getHostName();
                sendCommand("HELO " + localhost + CRLF , 250);

                isConnected = true;
            }
        }

        /* Send the message. Write the correct SMTP-commands in the
           correct order. No checking for errors, just throw them to the
           caller.
                Command	Reply Code
                DATA		354
                HELO		250
                MAIL FROM	250
                QUIT		221
                RCPT TO		250 */
        public void send(Envelope envelope) throws IOException {
		/* Send all the necessary commands to send a message. Call
		   sendCommand() to do the dirty work. Do _not_ catch the
		   exception thrown from sendCommand(). */

            /* Send MAIL FROM... */
            sendCommand("MAIL FROM: " + envelope.Sender + CRLF,250);
            /* Send RCPT TO ... */
            for(String recipient: envelope.Recipient)
                sendCommand("RCPT TO: " + recipient + CRLF,250);
            /* Send the DATA... */
            String dataToSend = envelope.Message.toString();
            sendCommand("DATA"+CRLF,354);
            sendCommand(dataToSend + CRLF + "." + CRLF,250);
        }

        /* Close the connection. First, terminate on SMTP level, then
           close the socket. */
        public void close() {
            isConnected = false;
            try {
                sendCommand( "QUIT" + CRLF,221 );
                connection.close();
            } catch (IOException e) {
                System.out.println("Unable to close connection: " + e);
                isConnected = true;
            }
        }

        /*  Send an SMTP command to the server. Check that the reply code is
            what is is supposed to be according to RFC 821. */
        private void sendCommand(String command, int rc) throws IOException {
            int src;// This is the Servers response code variable

            /* Write command to server and read reply from server. */
            toServer.writeBytes(command);
            System.out.println(command);//used for testing
            String serverReply = fromServer.readLine();
            System.out.println(serverReply);//used for testing
            src = parseReply(serverReply);

		/* Check that the server's reply code is the same as the parameter
		   rc. If not, throw an IOException. */
            if(src != rc){
                throw new IOException("Reply code mismatch.");
            }
        }

        /* Parse the reply line from the server. Returns the reply code. */
        private int parseReply(String reply) {
            String[] splitMessage = reply.split(" ");
            int code = -1;
            if( splitMessage.length > 0){
                code = Integer.parseInt(splitMessage[0]);
            }
            return code;
        }

        /* Destructor. Closes the connection if something bad happens. */
        protected void finalize() throws Throwable {
            if(isConnected) {
                close();
            }
            super.finalize();
        }
    }
