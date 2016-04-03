/**
 * 
 */
package com.chamika.fbmsgbackup.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import android.content.Context;
import android.os.Environment;

import com.chamika.fbmsgbackup.Constants;

/**
 * @author Chamika
 */
public class FileLoader {
	// private final static String TAG = "FileLoader";

	/**
	 * Save a Serializable to disk
	 * 
	 * @param filename
	 * @param object
	 * @return true if success
	 */
	public static boolean saveObject(Context context, String filename, Serializable object) {
		try {
			FileOutputStream fout = context.openFileOutput(filename, Context.MODE_PRIVATE);
			ObjectOutputStream oos = new ObjectOutputStream(fout);
			oos.writeObject(object);
			oos.close();
			fout.close();
			return true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Retrieve Serializable object from provided filename
	 * 
	 * @param filename
	 * @return Serializable object of the file, null if error happens
	 */
	public static Serializable loadObject(Context context, String filename) {
		Serializable serializable = null;
		try {
			FileInputStream file = context.openFileInput(filename);
			BufferedInputStream buffer = new BufferedInputStream(file);
			ObjectInput input = new ObjectInputStream(buffer);

			serializable = (Serializable) input.readObject();
			input.close();
			buffer.close();
			file.close();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return serializable;
	}

	public static String getFilePrefix() {
		return Environment.getExternalStorageDirectory() + Constants.BACKUP_FOLDER + File.separator;
	}
}
