import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ConnHandle implements Runnable
{
	public Socket socket;
	
	@Override
	public void run()
	{
		try
		{
			BufferedReader dataIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			while(!dataIn.ready()) try{Thread.sleep(100);}catch(InterruptedException e){}
			String pageLine = dataIn.readLine();
			String page[] = pageLine.split(" ");
			
			String wantedPage = null;
			
			if(page[0].equals("GET"))
			{
				if(page[2].startsWith("HTTP/"))
				{
					wantedPage = page[1]; //TODO: html unescape
				}
				else
				{
					System.out.println("Discarding bad request: "+pageLine);
					while(true)
					{
						String next = dataIn.readLine();
						if(next.equals("")) break;
						System.out.println(next);
					}
					socket.close();
					return;
				}
			}
			else
			{
				System.out.println("Discarding bad request: "+pageLine);
				/*while(true)
				{
					String next = dataIn.readLine();
					if(next == null || next.equals("")) break;
					System.out.println(next);
				}*/
				socket.close();
				return;
			}
			if(wantedPage == null)
			{
				System.out.println("No page was requested!");
				socket.close();
				return;
			}
			ArrayList<String> pageData = new ArrayList<String>();
			Map<String, String> extras = new HashMap<String, String>();
			if(wantedPage.startsWith("/")) wantedPage = wantedPage.substring(1);
			boolean doingExtras = false;
			while(true)
			{
				if(wantedPage.length() == 0 || wantedPage.equals('/')) break;
				if(doingExtras)
				{
					int pos = wantedPage.indexOf('&');
					int pos2 = wantedPage.indexOf('=');
					if(pos == -1)
					{
						String data = wantedPage;
						String index = data.substring(0, pos2);
						String value = data.substring(pos2+1);
						extras.put(index, value);
						break;
					}
					else
					{
						if(pos2 > pos) throw new IOException("Invalid page requested!");
						String data = wantedPage.substring(0, pos);
						String index = data.substring(0, pos2);
						String value = data.substring(pos2+1);
						extras.put(index, value);
						wantedPage = wantedPage.substring(pos+1);
					}
				}
				else
				{
					int pos = wantedPage.indexOf('/');
					int pos2 = wantedPage.indexOf('?');
					if(pos == -1)
					{
						if(pos2 == -1)
						{
							pageData.add(wantedPage);
							break;
						}
						else
						{
							if(pos2-1 > pos) pageData.add(wantedPage.substring(0, pos2));
							wantedPage = wantedPage.substring(pos2+1);
							doingExtras = true;
						}
					}
					else
					{
						if(pos2 == -1 || pos < pos2)
						{
							pageData.add(wantedPage.substring(0, pos));
							wantedPage = wantedPage.substring(pos+1);
						}
						else
						{
							pageData.add(wantedPage.substring(0, pos2));
							wantedPage = wantedPage.substring(pos2+1);
							doingExtras = true;
						}
					}
				}
			}
			page = pageData.toArray(new String[0]);
			while(true)
			{
				if(dataIn.readLine().equals("")) break;
			}
			PrintWriter dataOut = new PrintWriter(socket.getOutputStream(), true);
			if(page.length > 1)
			{
				if(page[0].equalsIgnoreCase("api"))
				{
					if(page[1].equalsIgnoreCase("verify"))
					{
						System.out.println("Key verify requested");
						dataOut.println("HTTP/1.1 200 OK");
						dataOut.println("Content-Type: text; charset=UTF-8");
						dataOut.println("");
						if(page.length > 2 && page[2].equalsIgnoreCase(Server.key))
						{
							dataOut.println("{\"valid\":\"Key validated.\"}");
						}
						else
						{
							dataOut.println("{\"error\":\"Invalid key provided.\"}");
						}
						dataOut.println("");
						socket.close();
						return;
					}
					if(page[1].equalsIgnoreCase("modpack"))
					{
						dataOut.println("HTTP/1.1 200 OK");
						dataOut.println("Content-Type: text; charset=UTF-8");
						dataOut.println("");
						if(page.length == 2)
						{
							getModpacks(dataOut);
							return;
						}
						if(page.length == 3)
						{
							getModpack(page[2], dataOut);
							return;
						}
						getModpack(page[2], page[3], extras.get("include"), dataOut);
						return;
					}
				}
				if(page[0].equalsIgnoreCase("data"))
				{
					getData(page, dataOut);
					return;
				}
			}
            if(page[0].equalsIgnoreCase("event.js")
            {
                //TODO: Stuff the site called:
                // /event.js?thread_slug=hexis_modpack_technic_platform&user_type=anon&referrer=http%3A%2F%2Fwww.technicpack.net%2Fmodpack%2Fdetails%2Fhexis-modpack.205018&theme=next&event=init_embed&thread=1235088989&forum=technic&forum_id=2197465&imp=38jobj318kohvs&prev_imp
                // /event.js?major_version=metadata&internal_organic=4&external_organic=0&promoted=0&display=false&placement=bottom&event=init_discovery&thread=1235088989&forum=technic&forum_id=2197465&imp=38jobj318kohvs&prev_imp
            }
			dataOut.println("HTTP/1.1 410 Gone");
			dataOut.println("");
			dataOut.println("");
			socket.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	private String getMD5(String string) throws Exception
	{
		MessageDigest md = MessageDigest.getInstance("MD5");
		InputStream is = new FileInputStream(new File(string));
		DigestInputStream dis = new DigestInputStream(is, md);
		byte[] buffer = new byte[1024*1024]; //1MB
		while(true)
		{
			int read = dis.read(buffer);
			if(read == -1) break;
			md.update(buffer, 0, read);
		}
		dis.close();
		byte[] digest = md.digest();
		return toHexString(digest);
	}
	
	private char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
	private String toHexString(byte[] bytes)
	{
		char[] hexChars = new char[bytes.length*2];
		int v;
		for (int j = 0; j < bytes.length; j++)
		{
			v = bytes[j] & 0xFF;
			hexChars[j*2] = hexArray[v>>>4];
			hexChars[j*2 + 1] = hexArray[v&0xF];
		}
		return new String(hexChars);
	}
	
	private void getModpacks(PrintWriter dataOut) throws IOException
	{
		System.out.println("Modpack list requested");
		//TODO: allow using mirror url for modpack files
		String reply = "{\"mirror_url\":\"http://THIS_SERVER_IP_HERE/data/\",\"modpacks\":{";
		File[] files = new File("modpacks").listFiles();
		boolean needsComma = false;
		for(int a = 0; a < files.length; a++)
		{
			if(files[a].isDirectory())
			{
				File[] subFiles = files[a].listFiles();
				for(int z = 0; z < subFiles.length; z++)
				{
					if(subFiles[z].isFile() && subFiles[z].getName().equals("info.txt"))
					{
						if(needsComma) reply += ",";
						needsComma = true;
						BufferedReader infoReader = new BufferedReader(new FileReader(subFiles[z]));
						String displayName;
						displayName = infoReader.readLine();
						infoReader.close();
						reply += "\""+files[a].getName()+"\":\""+displayName+"\"";
					}
				}
			}
		}
		reply += "}}";
		dataOut.println(reply);
		dataOut.println("");
		socket.close();
	}

	private void getModpack(String modPack, PrintWriter dataOut) throws IOException
	{
		File f = new File("modpacks/"+modPack);
		System.out.println("Modpack requested: "+modPack);
		if(f.isDirectory())
		{
			File infoFile = null;
			ArrayList<String> versions = new ArrayList<String>();
			File[] files = f.listFiles();
			for(int a = 0; a < files.length; a++)
			{
				if(files[a].isFile() && files[a].getName().equals("info.txt")) infoFile = files[a];
				if(files[a].isDirectory()) versions.add(files[a].getName());
			}
			if(infoFile == null)
			{
				dataOut.println("{\"error\":\"Internal error\"}");
				dataOut.println("");
				socket.close();
				return;
			}
			if(!infoFile.isFile())
			{
				dataOut.println("{\"error\":\"Internal error\"}");
				dataOut.println("");
				socket.close();
			}
			BufferedReader infoReader = new BufferedReader(new FileReader(infoFile));
			String displayName, url, recommended;
			displayName = infoReader.readLine();
			url = infoReader.readLine();
			recommended = infoReader.readLine();
			infoReader.close();
			String reply = "{\"name\":\""+modPack+"\",\"display_name\":\""+displayName+"\",\"url\":\""+url+"\"";
			reply += ",\"icon\":\"http://THIS_SERVER_IP_HERE/data/"+modPack+"/_/icon.png\"";
			reply += ",\"logo\":\"http://THIS_SERVER_IP_HERE/data/"+modPack+"/_/logo_180.png\"";
			reply += ",\"background\":\"http://THIS_SERVER_IP_HERE/data/"+modPack+"/_/background.jpg\"";
			String iconMD5 = null, logoMD5 = null, bgMD5 = null;
			try
			{
				iconMD5 = getMD5("modpacks/"+modPack+"/icon.png");
				logoMD5 = getMD5("modpacks/"+modPack+"/logo_180.png");
				bgMD5 = getMD5("modpacks/"+modPack+"/background.jpg");
			}
			catch(Exception e)
			{
				e.printStackTrace();
				if(iconMD5 == null) iconMD5 = "";
				if(logoMD5 == null) logoMD5 = "";
				if(bgMD5 == null) bgMD5 = "";
			}
			reply += ",\"icon_md5\":\""+iconMD5+"\"";
			reply += ",\"logo_md5\":\""+logoMD5+"\"";
			reply += ",\"background_md5\":\""+bgMD5+"\"";
			reply += ",\"recommended\":\""+recommended+"\"";
			reply += ",\"latest\":\""+recommended+"\"";
			reply += ",\"builds\":[";
			for(int a = 0; a < versions.size(); a++)
			{
				if(a > 0) reply += ",";
				reply += "\""+versions.get(a)+"\"";
			}
			dataOut.println(reply+"]}");
			dataOut.println("");
			socket.close();
			return;
		}
		else
		{
			dataOut.println("{\"error\":\"Modpack does not exist\"}");
			dataOut.println("");
			socket.close();
			return;
		}
	}
	
	private void getModpack(String modPack, String version, String includeValue, PrintWriter dataOut) throws IOException
	{
		System.out.println("Modpack requested: "+modPack+" V"+version);
		String[] includes = new String[0];
		if(includeValue != null) includes = includeValue.split(",");
		if(includes.length == 0)
		{
			includes = new String[]{"name","version","md5","url"};
		}
		else
		{
			boolean found = false;
			for(int a = 0; a < includes.length; a++)
			{
				if(includes[a].equals("mods"))
				{
					includes = new String[]{"name","version","md5","pretty_name","author","description","link"};
					found = true;
					break;
				}
			}
			if(!found)
			{
				String[] inc = new String[includes.length+3];
				inc[0] = "name";
				inc[1] = "version";
				inc[2] = "md5";
				for(int a = 0; a < includes.length; a++)
				{
					inc[a+3] = includes[a];
				}
				includes = inc;
			}
		}
		File f = new File("modpacks/"+modPack);
		if(!f.isDirectory())
		{
			dataOut.println("{\"error\":\"Modpack does not exist\"}");
			dataOut.println("");
			socket.close();
			return;
		}
		f = new File("modpacks/"+modPack+"/"+version);
		if(f.isDirectory())
		{
			File infoFile = new File("modpacks/"+modPack+"/"+version+"/info.txt");
			BufferedReader reader = new BufferedReader(new FileReader(infoFile));
			String mcVer = reader.readLine(), mcMD5 = reader.readLine(), forge = reader.readLine();
			reader.close();
			String reply = "{\"minecraft\":\""+mcVer+"\",\"minecraft_md5\":\""+mcMD5+"\",\"forge\":\""+forge+"\",\"mods\":[";
			File[] mods = new File("modpacks/"+modPack+"/"+version+"/mods").listFiles();
			if(mods == null)
			{
				dataOut.println("{\"error\":\"Internal error\"}");
				dataOut.println("");
				socket.close();
				return;
			}
			for(int a = 0; a < mods.length; a++)
			{
				reader = new BufferedReader(new FileReader(mods[a]));
				String name, ver, md5, prettyName, author, link, description;
				name = reader.readLine();
				ver = reader.readLine();
				md5 = reader.readLine();
				prettyName = reader.readLine();
				author = reader.readLine();
				link = reader.readLine();
				description = reader.readLine();
				while(reader.ready())
				{
					description += "<br>"+reader.readLine();
				}
				reader.close();
				if(a > 0) reply += ",";
				reply += "{";
				for(int z = 0; z < includes.length; z++)
				{
					if(z > 0) reply += ",";
					if(includes[z].equals("name")) reply += "\"name\":\""+name+"\"";
					else if(includes[z].equals("version")) reply += "\"version\":\""+ver+"\"";
					else if(includes[z].equals("md5")) reply += "\"md5\":\""+md5+"\"";
					else if(includes[z].equals("pretty_name")) reply += "\"pretty_name\":\""+prettyName+"\"";
					else if(includes[z].equals("author")) reply += "\"author\":\""+author+"\"";
					else if(includes[z].equals("description")) reply += "\"description\":\""+description+"\"";
					else if(includes[z].equals("link")) reply += "\"link\":\""+link+"\"";
					else if(includes[z].equals("url")) reply += "\"url\":\""+link+"\"";
					else
					{
						System.out.println("Unknown include: "+includes[z]);
						reply += "\""+includes[z]+"\":\"???\"";
					}
				}
				reply += "}";
			}
			reply += "]}";
			dataOut.println(reply);
			dataOut.println("");
			socket.close();
		}
		else
		{
			dataOut.println("{\"error\":\"Build does not exist\"}");
			dataOut.println("");
			socket.close();
		}
	}
	
	private void getData(String[] data, PrintWriter dataOut) throws IOException
	{
		File f = new File("modpacks/"+data[1]+"/"+data[3]);
		if(f.isFile())
		{
			dataOut.println("HTTP/1.1 200 OK");
			OutputStream s = socket.getOutputStream();
			FileInputStream s2 = new FileInputStream(f);
			int len = (int)f.length();
			int pos = 0;
			byte[] fileData = new byte[len];
			while(pos < len)
			{
				int val = s2.read(fileData, pos, len-pos);
				if(val == -1)
				{
					dataOut.println("Content-Type: text; charset=UTF-8");
					dataOut.println("");
					dataOut.println("An error occured!");
					dataOut.println("");
					socket.close();
					s2.close();
					return;
				}
				pos += val;
			}
			s2.close();
			dataOut.println("");
			s.write(fileData);
			dataOut.println("");
			socket.close();
			return;
		}
		dataOut.println("HTTP/1.1 410 Gone");
		dataOut.println("");
		dataOut.println("");
		socket.close();
	}
}