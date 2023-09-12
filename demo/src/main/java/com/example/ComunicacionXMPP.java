package com.example;

import java.io.IOException;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jxmpp.stringprep.XmppStringprepException;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.chat2.IncomingChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;


public class ComunicacionXMPP {

    private AbstractXMPPConnection connection;
// Constructor to establish XMPP connection
    public ComunicacionXMPP(String host, int port, String domain) throws XmppStringprepException {
        XMPPTCPConnectionConfiguration config = XMPPTCPConnectionConfiguration.builder()
                .setHost(host)
                .setPort(port)
                .setXmppDomain(domain)
                .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)
                .setConnectTimeout(10000) // Increase the timeout to 10 seconds
                .build();

        connection = new XMPPTCPConnection(config);
    }

    // Logic to log in
    public boolean iniciarSesion(String username, String password) {
        try {
            connection.connect();
            connection.login(username, password);
            return true;
        } catch (SmackException | IOException | XMPPException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }
// Logic to close the connection
    public void cerrarConexion() {
        if (connection != null && connection.isConnected()) {
            connection.disconnect();
        }
    }

    private Chat chat;
// Logic to start a chat with a recipient
    public void iniciarChat(String recipient) throws SmackException.NotConnectedException, XmppStringprepException, InterruptedException {
        ChatManager chatManager = ChatManager.getInstanceFor(connection);
        EntityBareJid jid = JidCreate.entityBareFrom(recipient ); //Addres of the other user
        chat = chatManager.chatWith(jid);

        // listener to incoming message
        chatManager.addIncomingListener(new IncomingChatMessageListener() {
            @Override
            public void newIncomingMessage(EntityBareJid from, Message message, Chat chat) {
                String body = message.getBody();
                System.out.println(from + ": " + body);
            }
        });

    }

    public void enviarMensaje(String message, String message2) throws SmackException.NotConnectedException, InterruptedException {
        Message newMessage = new Message();
        newMessage.setBody(message);
        chat.send(newMessage);
    }


    public void cerrarChat() {
        // Close Smack
    }

}
