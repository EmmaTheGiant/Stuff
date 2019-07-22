package zeale.apps.stuff.api.chatroom.connections.server;

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.net.ServerSocket;

import org.alixia.javalibrary.networking.sockets.Client;
import org.alixia.javalibrary.networking.sockets.Server;
import org.alixia.javalibrary.util.Box;

import zeale.apps.stuff.api.chatroom.ChatroomAPI;
import zeale.apps.stuff.api.chatroom.connections.client.ChatroomClient;
import zeale.apps.stuff.api.chatroom.connections.messages.EndConnectionMessage;
import zeale.apps.stuff.api.chatroom.events.Event;
import zeale.apps.stuff.api.chatroom.events.EventManager;

/**
 * A server that runs on this machine, awaiting for connection attempts from
 * {@link ChatroomClient}s. When a successful connection is established, the
 * server waits for the {@link ChatroomClient} to send a {@link String}
 * representing supported protocol version information. Once the client sends
 * this, the server sends its supported protocol version back, and then the
 * server awaits any query or action from the client.
 * 
 * @author Zeale
 *
 */
public class ChatroomServer implements Closeable {

	private ChatroomConnectionListener listener;
	private final EventManager<Event> eventManager = new EventManager<>();

	public ChatroomServer() throws IOException {
		this(ChatroomAPI.getDefaultConnectionPort());
	}

	public ChatroomServer(int port) throws IOException {
		this(new ServerSocket(port));
	}

	public ChatroomServer(ServerSocket socket) {
		this(new Server(socket));
	}

	protected String getVersion() {
		return ChatroomAPI.getCurrentVersion();
	}

	protected boolean acceptVersion(String version) {
		return ChatroomAPI.getCurrentVersion().contentEquals(version);
	}

	public ChatroomServer(Server server) {
		listener = new ChatroomConnectionListener(server) {
			@Override
			protected void handleIncomingConnection(Client connection) {
				eventManager.fire(IncomingClientEvent.INCOMING_CLIENT_EVENT,
						new IncomingClientEvent(ChatroomServer.this, connection));// Incoming Connection.
				try {
					Box<Serializable> result = connection.read(5000);// Expect String with version information.
				} catch (ClassNotFoundException | IOException e) {
					connection.send(EndConnectionMessage.STREAM_ERROR_OCCURRED);
				}
			}
		};
	}

	/**
	 * Launches this {@link ChatroomServer} which opens it to accept connections, on
	 * a freshly created thread. Once a connection is accepted, <b>a new thread is
	 * automatically created to handle it</b>. On this thread, the user is and the
	 * {@link #handleUserConnected(ChatroomUser)} method is called, on that thread,
	 * with a new {@link ChatroomUser} object representing the newly connected user.
	 * 
	 * @throws IllegalStateException In case this {@link ChatroomServer} is already
	 *                               running.
	 */
	public void launch() throws IllegalStateException {
		listener.open();
	}

	@Override
	public void close() throws IOException {
		listener.close();
		listener = null;
	}

}
