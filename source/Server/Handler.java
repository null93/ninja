package Server;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import java.net.Socket;

/**
 * This class groups together all handlers for all types of packets.  All the handlers have passed
 * the caller Respond instance and the request in JSON java format.
 * @version     1.0.0
 * @university  University of Illinois at Chicago
 * @course      CS342 - Software Design
 * @category    Project #04 - Ninja: Chat Application
 * @package     Server
 * @author      Rafael Grigorian
 * @author      Byambasuren Gansukh
 * @license     GNU Public License <http://www.gnu.org/licenses/gpl-3.0.txt>
 */
public class Handler {

	/**
	 * This data member holds a reference to the parent Server instance that is the caller to this
	 * instance.  It is stored, because we want to user many useful function from that class.
	 * @var     Server          parent              The Server instance that spawned this instance
	 */
	private Server parent;

	private UsersDB users_db;

	/**
	 * This constructor simply saves the parent reference and thats all it does.  It also
	 * initializes the data base instances.
	 * @param   Server          parent              The Server instance that spawned this instance
	 */
	public Handler ( Server parent ) {
		// Save the parent reference internally
		this.parent = parent;
		this.users_db = new UsersDB();
	}

	/**
	 *
	 */
	protected void handleLogin ( Respond callback, JSONObject request ) {
		System.out.println ( "[LOGIN]\t\t" + request.toString () );

		String username = request.get("username").toString();
		String password = request.get("password").toString();
		JSONArray groups = GroupDB.getGroups(username);

		if(users_db.userLogin(username, password)) {
			// Add user connection to logged in array list
			this.parent.addClient ( username, callback );
			// Write a response back to client
			
			// callback.write ( "{\"type\":\"login\",\"status\":\"success\",\"public_key\":\"SERVER_KEY\",\"username\":\"NULL\",\"users\":[{\"username\":\"NULL\",\"online\":true},{\"username\":\"BennyS\",\"online\":true},{\"username\":\"TheHolyBeast\",\"online\":false},{\"username\":\"HypeBeast\",\"online\":false},{\"username\":\"Clouds\",\"online\":false},{\"username\":\"TamerS\",\"online\":false}],\"groups\":[{\"name\":\"Everybody\",\"hash\":\"0\",\"users\":[\"NULL\",\"BennyS\",\"TheHolyBeast\"],\"messages\":[{\"from\":\"TheHolyBeast\",\"timestamp\":\"04/04/2016 - 12:24:02\",\"message\":\"Hey!\"},{\"from\":\"Clouds\",\"timestamp\":\"04/04/2016 - 12:24:02\",\"message\":\"What up!\"},{\"from\":\"TamerS\",\"timestamp\":\"04/04/2016 - 12:24:02\",\"message\":\"@Unemployeed\"},{\"from\":\"BennyS\",\"timestamp\":\"04/04/2016 - 12:24:02\",\"message\":\"Ayyyye!\"},{\"from\":\"HypeBeast\",\"timestamp\":\"04/04/2016 - 12:24:02\",\"message\":\"What's Happening!\"},{\"from\":\"NULL\",\"timestamp\":\"04/04/2016 - 12:23:53\",\"message\":\"Hey what's up guys!\"}]},{\"name\":\"CS342\",\"hash\":\"SFVG67RE6GVS8SHCA7SCGDHSKAFIUFDSHAOW\",\"users\":[\"NULL\",\"BennyS\"],\"messages\":[{\"from\":\"NULL\",\"timestamp\":\"04/04/2016 - 12:27:22\",\"message\":\"What up Ben!\"},{\"from\":\"BennyS\",\"timestamp\":\"04/04/2016 - 12:24:02\",\"message\":\"Yo is the GUI done yet?\"},{\"from\":\"NULL\",\"timestamp\":\"04/04/2016 - 12:24:02\",\"message\":\"Yes ;)\"}]}]}" );
			// this.parent.sendAllClients ( "{\"type\":\"online\",\"username\":\"" + username + "\"}" );
			// return;

			callback.write ( this.successSync ( "login", username, groups ).toString () );
			System.out.println ( this.successSync ( "login", username, groups ).toString () );
		}
		else {
			callback.write ( this.failTemplate ( "login", "Failed to login! Username doesn't exist and/or wrong password!" ).toString () );
		}
		// Send everyone a message saying that you logged in
		this.parent.sendAllClients ( successToAll("online", username).toString() );
	}

	/**
	 *
	 */
	protected void handleCreate ( Respond callback, JSONObject request ) {

		System.out.println ( "[CREATE]\t\t" + request.toString () );

		// Get the username and password
		String username = request.get ( "username" ).toString ();
		String password = request.get ( "password" ).toString ();
		JSONArray groups = GroupDB.getGroups(username);
		// Check to see if we successfully created a user
		if ( users_db.userAdd ( username, password ) ) {
			// Add user connection to logged in array list
			this.parent.addClient ( username, callback );

			// Send response to user
			callback.write ( this.successSync ( "create", username, groups).toString () );
		}
		// Otherwise, we failed
		else {
			// Send failed JSON message
			callback.write (
				// Create a template using the fail template generator
				this.failTemplate ( "create", "Failed to create account! User already exists.\n" +
					"Try a different username.").toString ()
			);
		}
		// Send everyone a message saying that you logged in
		this.parent.sendAllClients ( successToAll("created", username).toString() );
	}

	/**
	 *
	 */
	protected void handleMessage ( Respond callback, JSONObject request ) {
		System.out.println ( "[MESSAGE]\t" + request.toString () );

		String groupname = request.get("name").toString();
		String group_hash = request.get("hash").toString();
		JSONArray users = (JSONArray) request.get("users");
		GroupDB newGroup = new GroupDB(group_hash, groupname, users);

		String from = request.get("from").toString();
		String timestamp = request.get("timestamp").toString();
		String message = request.get("message").toString();

		newGroup.addMessage(from, timestamp, message);

		if ( users.contains ( "Everybody" ) ) {
			this.parent.sendAllClients ( request.toString () );
		}
		else {
			for ( Tuple client : this.parent.clients ) {
				if ( users.contains ( client.first ().toString () ) ) {
					Respond target = ( Respond ) client.second ();
					target.write ( request.toString () );
				}
			}
		}
	}

	/**
	 *
	 */
	protected void handleLogout ( Respond callback, JSONObject request ) {
		System.out.println ( "[LOGOUT]\t\t" + request.toString () );
		// Remove user connection from logged in client array list
		this.parent.removeClient ( request.get ( "username" ).toString () );

		callback.write ( this.parent.clientsOnline ().toString () );
	}

	/**
	 * 
	 */
	protected JSONObject successSync ( String type, String username, JSONArray groups )
	{
		JSONObject result = new JSONObject();

		result.put("type", type);
		result.put("status", "success");
		result.put("username", username);

		JSONArray users = new JSONArray();

		for(User x : users_db.USERS)
		{
				JSONObject user = new JSONObject();

				String other_user = x.getUsername();

				user.put("username", other_user);

				if(parent.findClient(other_user) == null)
					user.put("online", false);
				else
					user.put("online", true);

				users.add(user);
		}
		result.put("users", users);
		result.put("groups", groups);
		return result;
	}

	protected JSONObject successToAll( String type, String username)
	{
		JSONObject result = new JSONObject();

		result.put("type", type);
		result.put("username", username);

		return result;
	}

	/**
	 * This function creates a failed json object and uses the passed type and message and returns
	 * the JSONObject.  Please note that you can easily turn this to a json string by using the
	 * toString () function.
	 * @param   String          type                The type of response it will be
	 * @param   String          message             The error message to include
	 * @return  JSONObject                          A JSON java object with populated information
	 */
	private JSONObject failTemplate ( String type, String message ) {
		// Initialize a json object
		JSONObject json = new JSONObject ();
		// Put in the key pair values
		json.put ( "type", type );
		json.put ( "status", "fail" );
		json.put ( "message", message );
		// Return the JSON object
		return json;
	}

}