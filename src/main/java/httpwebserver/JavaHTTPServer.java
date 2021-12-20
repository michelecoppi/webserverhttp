package httpwebserver;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.StringTokenizer;


public class JavaHTTPServer implements Runnable{ 
	
	static final File WEB_ROOT = new File("./src/main/resources");
	//file che invia di default 
	static final String DEFAULT_FILE = "index.html";
	//quando non trova il file
	static final String FILE_NOT_FOUND = "404.html";
	//quando il metodo non è supportato 
	static final String METHOD_NOT_SUPPORTED = "not_supported.html";
	// porta in ascolto
	static final int PORT = 8080;
	private int statuscode;
	
	// dettagli aggiuntivi su che cosa sta facendo il programma
	static final boolean verbose = true;
	
	// Socket per la connessione 
	private Socket connect;
	
	public JavaHTTPServer(Socket c) {
		connect = c;
	}
	
	public static void main(String[] args) {
		try {
			ServerSocket serverConnect = new ServerSocket(PORT);
			System.out.println("Server started.\nListening for connections on port : " + PORT + " ...\n");
			
			// loop infinito per rimanere in ascolto
			while (true) {
				JavaHTTPServer myServer = new JavaHTTPServer(serverConnect.accept());
				
				if (verbose) {
					System.out.println("Connecton opened. (" + new Date() + ")");
				}
				
				// thread per gestire la connessione del client
				Thread thread = new Thread(myServer);
				thread.start();
			}
			
		} catch (IOException e) {
			System.err.println("Server Connection error : " + e.getMessage());
		}
	}

	@Override
	public void run() {
		// gestione del cient 
		BufferedReader in = null; PrintWriter out = null; BufferedOutputStream dataOut = null;
		String fileRequested = null;
		
		try {
			// input dal client
			in = new BufferedReader(new InputStreamReader(connect.getInputStream()));
			// output al client dell'header 
			out = new PrintWriter(connect.getOutputStream());
			// output al client dei dati richiesti
			dataOut = new BufferedOutputStream(connect.getOutputStream());
			
			// otteniamo la prima riga della risposta del cient
			String input = in.readLine();
			// analizziamo la richiesta con un tokenizer
			StringTokenizer parse = new StringTokenizer(input);
			String method = parse.nextToken().toUpperCase(); // otteniamo il metodo HTTP dal client 
			// ottieniamo file richiesto
			fileRequested = parse.nextToken().toLowerCase();
			
			// supportati solo i metodi get e post
			if (!method.equals("GET")  &&  !method.equals("HEAD")) {
				if (verbose) {
					System.out.println("501 Not Implemented : " + method + " method.");
				}
				
				// ritorniamo il file non supportato al client
				File file = new File(WEB_ROOT, METHOD_NOT_SUPPORTED);
				int fileLength = (int) file.length();
				String contentMimeType = "text/html";
				//read content to return to client
				byte[] fileData = readFileData(file, fileLength);
					
				statuscode=501;
				MandaFile(out, dataOut, file, fileLength, contentMimeType, statuscode);
				
			} else {
				if(fileRequested.endsWith("/")){
					fileRequested += DEFAULT_FILE;
					File file = new File(WEB_ROOT, fileRequested);
					if(file.isFile()){
						statuscode=200;
						int fileLength = (int) file.length();
						String content = getContentType(fileRequested);
						
						if (method.equals("GET")) { // GET method so we return content
							MandaFile(out, dataOut, file, fileLength, content, statuscode);
						}
						if (verbose) {
							System.out.println("File " + fileRequested + " of type " + content + " returned");
						}
						
					}else{
						statuscode = 404;
						File fileErr = new File(WEB_ROOT, FILE_NOT_FOUND);
						int fileLength = (int) file.length();
						String content = getContentType(fileRequested);

						MandaFile(out, dataOut, fileErr, fileLength, content, statuscode);
					}
				} else {
					File file = new File(WEB_ROOT, fileRequested);
					if(file.isFile()){
						statuscode=200;
						int fileLength = (int) file.length();
						String content = getContentType(fileRequested);
						
						if (method.equals("GET")) { // GET method so we return content
							MandaFile(out, dataOut, file, fileLength, content, statuscode);
						}
						if (verbose) {
							System.out.println("File " + fileRequested + " of type " + content + " returned");
						}
						
					}else{
						statuscode = 301;
						fileRequested+="/";
						out.println("HTTP/1.1 " + statuscode + " Location Changed");
						out.println("Location " + fileRequested);
						out.flush();
					}
				}

				File file = new File(WEB_ROOT, fileRequested);
				statuscode=200;
				int fileLength = (int) file.length();
				String content = getContentType(fileRequested);
				if (method.equals("GET")) { // GET method so we return content
					MandaFile(out, dataOut, file, fileLength, content, statuscode);
				}
				
				if (verbose) {
					System.out.println("File " + fileRequested + " of type " + content + " returned");
				}
				
			}
			
		} catch (FileNotFoundException fnfe) {
			try {
				fileNotFound(out, dataOut, fileRequested);
			} catch (IOException ioe) {
				System.err.println("Error with file not found exception : " + ioe.getMessage());
			}
			
		} catch (IOException ioe) {
			System.err.println("Server error : " + ioe);
		} finally {
			try {
				in.close();
				out.close();
				dataOut.close();
				connect.close(); // chiusura socket
			} catch (Exception e) {
				System.err.println("Error closing stream : " + e.getMessage());
			} 
			
			if (verbose) {
				System.out.println("Connection closed.\n");
			}
		}
		
		
	}
	
	private byte[] readFileData(File file, int fileLength) throws IOException {
		FileInputStream fileIn = null;
		byte[] fileData = new byte[fileLength];
		
		try {
			fileIn = new FileInputStream(file);
			fileIn.read(fileData);
		} finally {
			if (fileIn != null) 
				fileIn.close();
		}
		
		return fileData;
	}
	
	// ritorna tipi di MIME supportati
	private String getContentType(String fileRequested) {
		if (fileRequested.endsWith(".htm")  ||  fileRequested.endsWith(".html"))
			return "text/html";
		else
			return "text/plain";
	}
	
	private void fileNotFound(PrintWriter out, OutputStream dataOut, String fileRequested) throws IOException {
		File file = new File(WEB_ROOT, FILE_NOT_FOUND);
		int fileLength = (int) file.length();
		String content = "text/html";
		byte[] fileData = readFileData(file, fileLength);
		statuscode=404;
	    try{
			MandaFile(out, dataOut, file, fileLength, content, statuscode);
		}catch(IOException ioe){
			System.err.println("Error with file not found exception : " + ioe.getMessage());
		}
		if (verbose) {
			System.out.println("File " + fileRequested + " not found");
		}
	}
	
     
	private void MandaFile(PrintWriter out, OutputStream dataOut, File file, int fileLength, String content, int code) throws IOException{
		byte[] fileData = readFileData(file, fileLength);
		if(code == 404){
			out.println("HTTP/1.1 " + code + " File non trovato");
		}else if(code==501){
			out.println("HTTP/1.1 " + code + "  Non implementato");
		} else if(code==200){
			out.println("HTTP/1.1 " + code + "  OK");
		}
		out.println("Server: Java HTTP Server from SSaurel : 1.0");
		out.println("Date: " + new Date());
		out.println("Content-type: " + content);
		out.println("Content-length: " + fileLength);
		out.println(); 
		out.flush(); 
		
		dataOut.write(fileData, 0, fileLength);
		dataOut.flush();
	}


}
