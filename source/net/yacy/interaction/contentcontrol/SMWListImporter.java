package net.yacy.interaction.contentcontrol;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;

import net.yacy.kelondro.logging.Log;

import org.json.simple.parser.ContentHandler;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class SMWListImporter implements Runnable, ContentHandler{

	// Importer Variables
	private final ArrayBlockingQueue<SMWListRow> listEntries;
    private final Reader importFile;

    private SMWListRow row;
    private final JSONParser parser;

    // Parser Variables
	private final StringBuilder value;
	private final StringBuilder key;
	private final HashMap<String,String> obj;

	private Boolean isElement;

	public SMWListImporter(final Reader importFile, final int queueSize) {
        this.listEntries = new ArrayBlockingQueue<SMWListRow>(queueSize);
        this.importFile = importFile;

        this.row = new SMWListRow();

        this.parser = new JSONParser();

	    this.value = new StringBuilder(128);
	    this.key = new StringBuilder(16);
	    this.obj = new HashMap<String,String>();

		this.isElement = false;

	}

	@Override
    public void startJSON() throws ParseException, IOException {
	}

	@Override
    public void endJSON() throws ParseException, IOException {
	}

	@Override
    public boolean startArray() throws ParseException, IOException {
		final String key = this.key.toString();

		if (key.equals("items")) {

			this.isElement = true;

		}
		return true;
	}

	@Override
    public boolean endArray() throws ParseException, IOException {

		return true;
	}

	@Override
    public boolean startObject() throws ParseException, IOException {

		return true;
	}

	@Override
    public boolean endObject() throws ParseException, IOException {

		if(this.isElement) {

			for (Entry<String, String> e: this.obj.entrySet()) {
				this.row.add (e.getKey(), e.getValue());
			}
			try {
				this.listEntries.put(this.row);
				//this.count++;
			} catch (InterruptedException e) {
				Log.logException(e);
			}
			this.obj.clear();
			this.row = new SMWListRow();
		}

		return true;
	}

	@Override
    public boolean startObjectEntry(String key) throws ParseException, IOException {
			this.key.setLength(0);
			this.key.append(key);

		return true;
	}

	@Override
    public boolean primitive(Object value) throws ParseException, IOException {

			this.value.setLength(0);
			if(value instanceof java.lang.String) {
				this.value.append((String)value);
			} else if(value instanceof java.lang.Boolean) {
				this.value.append(value);
			} else if(value instanceof java.lang.Number) {
				this.value.append(value);
			}

		return true;
	}

	@Override
    public boolean endObjectEntry() throws ParseException, IOException {

			final String key = this.key.toString();
			final String value = this.value.toString();

			this.obj.put(key, value);

		return true;
	}

	@Override
    public void run() {
		try {
			Log.logInfo("SMWLISTSYNC", "Importer run()");
			this.parser.parse(this.importFile, this, true);

		} catch (IOException e) {
			Log.logException(e);
		} catch (ParseException e) {
			Log.logException(e);
		} finally {

			try {
				Log.logInfo("SMWLISTSYNC", "Importer inserted poison pill in queue");
				this.listEntries.put(SMWListRow.POISON);
			} catch (InterruptedException e) {
				Log.logException(e);
			}
		}
	}

    public SMWListRow take() {
        try {
            return this.listEntries.take();
        } catch (InterruptedException e) {
            Log.logException(e);
            return null;
        }
    }
}
