package com.cs51.seam;

import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.cli.Options;

/**
 * Servlet implementation class Main
 */
public class Main extends HttpServlet {
	private static final long serialVersionUID = 1L;

   
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		System.out.println("has get request");
		doPost(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		System.out.println("has post request");
		
		
		/*******************************************************************/
		String saveFile    = "";
		File             ff = null;
		String contentType = request.getContentType();
		if((contentType != null)&&(contentType.indexOf("multipart/form-data") >= 0)){
		   DataInputStream dis            = new DataInputStream(request.getInputStream());
		   int             formDataLength = request.getContentLength();
		   byte            dataBytes[]    = new byte[formDataLength];
		   int byteRead       = 0;
		   int totalBytesRead = 0;
		   while(totalBytesRead < formDataLength){
		      byteRead = dis.read(dataBytes, totalBytesRead,formDataLength);
		      totalBytesRead += byteRead;
		   }
		   String file = new String(dataBytes);
		   saveFile    = file.substring(file.indexOf("filename=\"") + 10);
		   saveFile    = saveFile.substring(0, saveFile.indexOf("\n"));
		   saveFile    = saveFile.substring(saveFile.lastIndexOf("\\") + 1,saveFile.indexOf("\""));
		   int    lastIndex = contentType.lastIndexOf("=");
		   String boundary  = contentType.substring(lastIndex + 1,contentType.length());
		   int pos;
		   pos = file.indexOf("filename=\"");
		   pos = file.indexOf("\n", pos) + 1;
		   pos = file.indexOf("\n", pos) + 1;
		   pos = file.indexOf("\n", pos) + 1;
		   int boundaryLocation = file.indexOf(boundary, pos) - 4;
		   int startPos         = ((file.substring(0, pos)).getBytes()).length;
		   int endPos           = ((file.substring(0, boundaryLocation)).getBytes()).length;
		                ff = new File("/Users/gregdicristofaro/Desktop/hello/"+saveFile);
		   FileOutputStream fileOutStream = new FileOutputStream(ff);
		   fileOutStream.write(dataBytes, startPos, (endPos - startPos));
		   fileOutStream.flush();
		   fileOutStream.close();
		}
			/*******************************************************************/
		
		BufferedImage img;
		try {
			InputStream is = (InputStream)request.getInputStream();
			img = Utils.getImage(ff);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error loading image");
			System.out.println();
			return;
		}

		System.out.println("TESTING...");
		Options opt = new Options();
		
		SeamCarve carver = new SeamCarve(img);
		String src = null;
		try {
			//Utils.writeImage(carver.getImage(), "png", Args[1]);
			// src= Utils.encodeImage(carver.getImage(), "png");

			//img = Utils.getImage(Args[0]);
			
		} catch (Exception e) {
			System.out.println("Error exporting image");
			System.out.println();
			return;
		}
		
		/*if (Args.length == 3) {
			try {
				File[] stuff = carver.getAnimPics();
				System.out.println("got stuff");
				//Utils.createMovie(carver.getEnergyGetter().getAnimPics(), 32, new File(Args[2]));
				Utils.createMovie(stuff, 32, new File(Args[2]));

			} catch (Exception e) {
				System.out.println("Error exporting movie: " + e.getMessage());
			}		
		}*/
		
		
		//get the list of files to be displayed
		//File[] files = carver.getAnimPics();
		//File[] files=null;
		Utils.writeResponse(response, 
				new File[]{Utils.imageToTempFile(carver.getSeamImage())},
				img);
	

		
	}

}
