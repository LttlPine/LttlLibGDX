package com.lttlgames.helpers;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ProxyPrintStream extends PrintStream
{
	private PrintStream fileStream;
	private PrintStream originalPrintStream;

	public ProxyPrintStream(PrintStream out, String FilePath)
	{
		super(out);
		originalPrintStream = out;
		try
		{
			FileOutputStream fout = new FileOutputStream(FilePath, true);
			fileStream = new PrintStream(fout);

			// print out date
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss");
			String date = dateFormat.format(new Date());
			fileStream.println("");
			fileStream.println("");
			fileStream.println(date + ":");
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
	}

	public void print(final String str)
	{
		originalPrintStream.print(str);
		fileStream.print(str);
	}

	public void println(final String str)
	{
		originalPrintStream.println(str);
		fileStream.println(str);
	}
}