package com.example;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.io.File;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackException.NotLoggedInException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.iqregister.AccountManager;
import org.jivesoftware.smackx.vcardtemp.packet.VCard;
import org.jxmpp.stringprep.XmppStringprepException;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.chat2.IncomingChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Localpart;
import org.jxmpp.jid.parts.Resourcepart;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.RosterGroup;
import org.jivesoftware.smack.roster.RosterListener;
import org.jivesoftware.smackx.vcardtemp.VCardManager;
import org.jivesoftware.smackx.vcardtemp.packet.VCard;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.muc.MultiUserChatException.NotAMucServiceException;

public class ComunicacionXMPP {

    private AbstractXMPPConnection connection;
// Constructor to establish XMPP connection
    public ComunicacionXMPP(String host, int port, String domain) throws XmppStringprepException {
        XMPPTCPConnectionConfiguration config = XMPPTCPConnectionConfiguration.builder()
                .setHost(host)
                .setPort(port)
                .setXmppDomain(domain)
                .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)
                .build();

        connection = new XMPPTCPConnection(config);
    }

    // Logic to log in
    public boolean iniciarSesion(String username, String password) {
        try {
            connection.connect();
            connection.login(username, password);
            iniciarEscuchaNotificaciones();
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
        EntityBareJid jid = JidCreate.entityBareFrom(recipient + "@alumchat.xyz"); //Addres of the other user
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

    public void enviarMensaje(String message) throws SmackException.NotConnectedException, InterruptedException {
        Message newMessage = new Message();
        newMessage.setBody(message);
        chat.send(newMessage);
    }


    public void cerrarChat() {
        // Close Smack
    }

    // Logic to register a new account
    public boolean registrarCuenta(String newUser, String newPassword) {
    try {
        AccountManager accountManager = AccountManager.getInstance(connection);
        accountManager.sensitiveOperationOverInsecureConnection(true);
        accountManager.createAccount(Localpart.from(newUser), newPassword);
        return true;
    } catch (SmackException | InterruptedException | XMPPException | XmppStringprepException e) {
        e.printStackTrace();
        return false;
        }
    }
    // Logic to delete the account
    public boolean eliminarCuenta() {
        try {
            AccountManager accountManager = AccountManager.getInstance(connection);
            accountManager.deleteAccount();
            return true;
        } catch (SmackException | InterruptedException | XMPPException e) {
            e.printStackTrace();
            return false;
        }
    }
    // Logic to get contacts and their states
    public Map<Jid, Presence> obtenerContactosYEstados() {
    Roster roster = Roster.getInstanceFor(connection);
    roster.setRosterLoadedAtLogin(true); // Charge the roster when log in

    Map<Jid, Presence> contactosYEstados = new HashMap<>();

    for (RosterEntry entry : roster.getEntries()) {
        Presence presence = roster.getPresence(entry.getJid());
        contactosYEstados.put(entry.getJid(), presence);
    }

    return contactosYEstados;
    }
    // Logic to add a contact
    public void agregarContacto(String contactJid) throws NotLoggedInException, NotConnectedException, InterruptedException, XMPPErrorException, SmackException {
        Roster roster = Roster.getInstanceFor(connection);
        BareJid jid = JidCreate.bareFromOrNull(contactJid);
        roster.createItemAndRequestSubscription(jid, contactJid, null);
    }

    // Logic to get contact details using VCard
    public VCard obtenerVCard(String username) throws XmppStringprepException, InterruptedException, NotConnectedException, XMPPErrorException {
        EntityBareJid jid = JidCreate.entityBareFrom(username + "@alumchat.xyz");
        VCardManager vCardManager = VCardManager.getInstanceFor(connection);
    
        try {
            VCard vCard = vCardManager.loadVCard(jid);
            return vCard;
        } catch (SmackException.NoResponseException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Logic to join a group conversation
    public MultiUserChat unirseConversacionGrupal(String roomName) throws XmppStringprepException, InterruptedException, XMPPErrorException, SmackException.NotConnectedException, NotAMucServiceException, NoResponseException {
        MultiUserChatManager mucManager = MultiUserChatManager.getInstanceFor(connection);
        EntityBareJid roomJid = JidCreate.entityBareFrom(roomName + "@conference.alumchat.xyz");
        
        MultiUserChat muc = mucManager.getMultiUserChat(roomJid);
        Resourcepart resource = Resourcepart.from(connection.getUser().getLocalpart().toString());
        muc.join(resource);

        // listener
        muc.addMessageListener(new MessageListener() {
            @Override
            public void processMessage(Message message) {
                String sender = message.getFrom().getResourceOrEmpty().toString();
                String body = message.getBody();
                System.out.println("[" + sender + "]: " + body);
            }
        });


        return muc;
    }

    public void enviarMensajeGrupo(MultiUserChat muc, String message) throws SmackException.NotConnectedException, InterruptedException {
        Message newMessage = new Message();
        newMessage.setBody(message);
        muc.sendMessage(newMessage);
    }

    public void definirMensajePresencia(Presence.Mode presenceMode) {
        Presence presence = new Presence(Presence.Type.available);
        presence.setMode(presenceMode);
    
        try {
            connection.sendStanza(presence);
            System.out.println("Estado cambiado.");
        } catch (SmackException.NotConnectedException | InterruptedException e) {
            e.printStackTrace();
            System.out.println("Error al cambiar el estado.");
        }
    }

    public void mostrarOpcionesEstado() {
        System.out.println("Opciones de estado:");
        System.out.println("1. Disponible");
        System.out.println("2. Ausente");
        System.out.println("3. Ocupado");
        System.out.print("Elige una opción: ");
    
        Scanner scanner = new Scanner(System.in);
        int choice = scanner.nextInt();
        scanner.nextLine(); // Consume newline
    
        Presence.Mode presenceMode = Presence.Mode.available;
    
        switch (choice) {
            case 1:
                presenceMode = Presence.Mode.available;
                break;
            case 2:
                presenceMode = Presence.Mode.away;
                break;
            case 3:
                presenceMode = Presence.Mode.dnd;
                break;
            default:
                System.out.println("Opción inválida. Estableciendo como Disponible.");
        }
    
        definirMensajePresencia(presenceMode);
    }

    // Logic to send a notification
    public void enviarNotificacion(String recipientUsername, String notificationMessage) throws SmackException.NotConnectedException, InterruptedException, XmppStringprepException {
        EntityBareJid recipientJid = JidCreate.entityBareFrom(recipientUsername + "@alumchat.xyz");
        Message notification = new Message(recipientJid);
        notification.setBody(notificationMessage);
        connection.sendStanza(notification);
    }

    public void iniciarEscuchaNotificaciones() {
        ChatManager chatManager = ChatManager.getInstanceFor(connection);
        chatManager.addIncomingListener(new IncomingChatMessageListener() {
            @Override
            public void newIncomingMessage(EntityBareJid from, Message message, Chat chat) {
                System.out.println("Notificación de " + from + ": " + message.getBody());
            }
        });
    }

}
