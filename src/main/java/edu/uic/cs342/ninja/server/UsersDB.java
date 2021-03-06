package edu.uic.cs342.ninja.server;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.io.IOException;
import java.lang.Exception;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.lang.StringBuilder;

/**
 * This class is meant to create the user file. Only one to be made.
 * The file would in the form of JSON and it would contain the username and the hashed password
 * of every user that have created an account.
 *
 * @version     1.0.0
 * @university  University of Illinois at Chicago
 * @course      CS342 - Software Design
 * @category    Project #04 - Ninja: Chat Application
 * @package     Server
 * @author      Rafael Grigorian
 * @author      Byambasuren Gansukh
 * @license     The MIT License, see LICENSE.md
 */
public class UsersDB {

	/**
     * This internal data member to store all the users as User objects
     * @var     ArrayList<User>           USERS     	Holds all the users
     */
	protected  	static 	ArrayList<User> 	USERS;

	/**
     * This internal data member to store the FILEPATH, never changes hence static final bind
     * @var     String           FILEPATH           	Holds the filepath
     */
	private		static 	final 	String 		FILEPATH = "./data/users/user.db";

	/**
	 * This constructor creates the users database file if it doesn't exist.
	 * Otherwise it loads and populates USERS.
	 */
	public UsersDB () {
		// Instantiate USERS
		USERS = new ArrayList<User>();

		// Check whether the file is created or not, Create if it doesn't exist
		if(!createFileAndPath()) {
			// Print info message to the console
			System.out.println ( "loaded users" );
			//read all the contents and store internally (Username,password_hash)
			load();
		}
		else
			// Print info message to the console
			System.out.println ("Created users.db");
	}

	/**
	 * This function checks whether the file specified by FILEPATH exists or not.
	 * If it doesn't exist then it creates otherwise it doesn't do anything.
	 * @return  boolean		Returns true when the file doesn't exist, false otherwise.
	 */
	protected static boolean createFileAndPath()
	{
		// Create File object
		File file = new File(FILEPATH);

		// Create parent directories if file is child
		if(file.getParentFile() != null)
			file.getParentFile().mkdirs();

		try
		{
			// Create a new file
			// True if the file doesn't exist and successfully create the file
			// False otherwise.
			return file.createNewFile();
		}
		// Catch exceptions
		catch(Exception e)
		{
			// Print debug message
			System.out.println("UsersDB createFilePath(): " + e);
			return false;
		}
	}

	/**
	 * This function deletes the users.db file
	 * @return  boolean		Returns true if file is successfully deleted, false otherwise.
	 */
	protected static boolean deleteFile( )
	{
		// Create File object on FILEPATH
	   File file = new File(FILEPATH);

	   // Attempt to delete the file, return true if successful. False otherwise
	   return file.delete();
	}

	/**
	 * Checks whether a username exists in users.db
	 * @param   String 		username  			Name of the user to search for in users.db
	 * @return  boolean		Returns true if user searching for exists in the database.
	 * 						False, otherwise.
	 */
	protected boolean userExists ( String username ) {
		// Firstly lower case the username
		String inUsername_lowered = username.toLowerCase();

		// Traverse through USERS
		for(User user : USERS)
		{
			// Lower user's username
			String username_lowered = user.getUsername().toLowerCase();

			// Compare both lowered usernames. If equal return true since a user exists
			if(inUsername_lowered.equals(username_lowered))
				return true;
		}
		// User doesn't exist
		return false;
	}

	/**
	 * This function adds a user to the internal data USERS as well as update the new users info
	 * to the database.
	 * @param   String 		username  		Name of the new user to be added
	 * @param 	String 		password        Password of the new user to be added
	 * @return  boolean		Returns true if user is added, false otherwise.
	 */
	protected boolean userAdd ( String username, String password) {
		// Hash the password
		String password_hash = hash(password);

		// Check whether the user exists
		if(!userExists( username ))
		{
			// User doesn't exist thus add to USERS and update the users.db file
			USERS.add(new User(username, password_hash));
			update(username, password_hash);
			return true;
		}
		// User exists, can't add user
		return false;
	}

    /**
	 * This function basically determines whether a user provides correct login info.
	 * @param   String 		username  			Login name
	 * @param 	String 		passedPassword     	Login password
	 * @return  boolean		Returns true if login info is correct, false otherwise
	 */
	protected boolean userLogin ( String username, String passedPassword ) {
		// Hash the passed password
		String passedPassword_hash = hash(passedPassword);

		// Traverse USERS
		for(User user : USERS)
			// If corresponding username and password exists then return true
			if(user.getUsername().equals(username)
				&& user.getPassword_hash().equals(passedPassword_hash))
				return true;

		// Return false, wrong login info
		return false;
	}

	/**
	 * This function updates the database file with a username and a hashed password.
	 *
	 * Synchronized to make sure no two threads try to load at the same time.
	 * @param 		String 		username 			Username to be added to the users.db
	 * @param 		String 		password_hash 		Password to be added to the users.db
	 * @return  	void
	 */
	private synchronized void update (String username, String password_hash) {

		// Declare File objects
		FileWriter fileWriter = null;
		BufferedWriter buffWriter = null;
		PrintWriter printWriter = null;

		try
		{
			// Create file objects
			File file = new File(FILEPATH);
			fileWriter = new FileWriter(file, true);
			buffWriter = new BufferedWriter(fileWriter);
			printWriter = new PrintWriter(buffWriter);

			// Update file with username and password_hash
			printWriter.println(username + "\t" + password_hash);
		}
		// Catch exceptions
		catch(Exception e)
		{
			// Print debug message
			System.err.println("UsersDB update(): " + e);
		}
		finally
		{
			// Close out the printWriter
			if(printWriter != null)
				printWriter.close();
		}
	}

	/**
	 * This function loads in the contents of the file and populates USERS with User objects.
	 *
	 * Synchronized to make sure no two threads try to load at the same time.
	 * @return  void
	 */
	private synchronized void load()
	{
		try
		{
			// Create file objects
			FileReader fileReader = new FileReader(FILEPATH);
			BufferedReader buffReader = new BufferedReader(fileReader);

			// Used to store each line
			String line = null;

			// Read the contents of the file line by line
			while((line = buffReader.readLine()) != null)
			{
				//Skip empty lines
				if(!line.equals(""))
				{
					// Split line into two strings
					String[] lineContents = line.split("\\s+");
					String tempUsername = lineContents[0]; // Username
					String tempPassword = lineContents[1]; // Passwords

					// Create a User object
					User tempUser = new User(tempUsername, tempPassword);

					// Populate USERS
					USERS.add(tempUser);
				}
			}
		}
		// Catch exceptions
		catch(Exception e)
		{
			// Print debug message
			System.err.println("UsersDB load(): " + e);
		}
	}

	/**
	 * This function hashes a string, in our case a password.
	 * The hashing function will be the MD5 algorithm provided by a JAVA library Message Digest
	 *
	 * @param 	String 		password 			Password to be hashed
	 * @return  String 		password_hash 		Hashed password
	 */
	protected String hash ( String password ) {

		// Instantiate String to hold hashed password
		String password_hash = null;

		try
		{
			// Create a MessageDigest object on MD5 algorithm
			MessageDigest md = MessageDigest.getInstance("MD5");

			// Pass in password to the algorithm
			md.update(password.getBytes());

			// Retrieve the digested password in bytes
			byte[] passwordBytes = md.digest();

			// Create a StringBuilder to build the hashed password
			StringBuilder stringBuilder = new StringBuilder();

			// Retrieve each byte from passwordBytes and append to stringBuilder
			for(byte b : passwordBytes)
				stringBuilder.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));

			// Retrieve the hashed password
			password_hash = stringBuilder.toString();
		}
		// Catch exceptions
		catch(Exception e)
		{
			// Print debug message
			System.err.println("UsersDB hash(): " + e);
		}

		// Return hashed password
		return password_hash;
	}

}
