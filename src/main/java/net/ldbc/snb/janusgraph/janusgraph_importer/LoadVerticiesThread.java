package net.ldbc.snb.janusgraph.janusgraph_importer;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import org.apache.tinkerpop.gremlin.structure.T;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.JanusGraphTransaction;

public class LoadVerticiesThread extends Thread{

	private JanusGraph graph;
	private String[] colNames;
	private String idLabel;
	private String entityName;
	private long txMaxRetries;
	private List<String> threadLines;
	private long lineCount;
	
	public LoadVerticiesThread(
			JanusGraph graph,
			String[] colNames,
			String idLabel,
			String entityName,
			long txMaxRetries,
			List<String> threadLines,
			long lineCount
			) {
		this.graph = graph;
		this.colNames = colNames;
		this.idLabel = idLabel;
		this.entityName = entityName;
		this. threadLines = threadLines;
		this.lineCount = lineCount;
	}

	@Override
	public void run() {

		SimpleDateFormat birthdayDateFormat = new SimpleDateFormat("yyyy-MM-dd");
		birthdayDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

		SimpleDateFormat creationDateDateFormat = 
				new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
		creationDateDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

		boolean txSucceeded = false;
		int txFailCount = 0;
		String[] lines = threadLines.toArray(new String[0]);
		do {
			JanusGraphTransaction tx = graph.newTransaction();
			for (int i = 0; i < lines.length; i++) {

				String line = lines[i];

				String[] colVals = line.split("\\|");
				HashMap<Object, Object> propertiesMap = new HashMap<>();

				for (int j = 0; j < colVals.length; ++j) {
					if (colNames[j].equals("id")) {
						propertiesMap.put(idLabel, Long.parseLong(colVals[j]));
					} else if (colNames[j].equals("birthday")) {
						try {
							propertiesMap.put(colNames[j],
									birthdayDateFormat.parse(colVals[j]).getTime());
						} catch (ParseException e) {
							e.printStackTrace();
						}
					} else if (colNames[j].equals("creationDate")) {
						try {
							propertiesMap.put(colNames[j], 
									creationDateDateFormat.parse(colVals[j]).getTime());
						} catch (ParseException e) {
							e.printStackTrace();
						}
					}else if (colNames[j].equals("length")) {
						propertiesMap.put(colNames[j], Integer.parseInt(colVals[j]));
					}else {
						propertiesMap.put(colNames[j], colVals[j]);
					}
				}

				propertiesMap.put(T.label, entityName);

				List<Object> keyValues = new ArrayList<Object>();
				propertiesMap.forEach((key, val) -> {
					keyValues.add(key);
					keyValues.add(val);
				});

				tx.addVertex(keyValues.toArray());

			}

			try {
				tx.commit();
				txSucceeded = true;
			} catch (Exception e) {
				txFailCount++;
			}

			if (txFailCount > txMaxRetries) {
				throw new RuntimeException(String.format(
						"ERROR: Transaction failed %d times, (file lines [%d,%d])" +  
								"aborting...", txFailCount, lineCount + 2, (lineCount + 2) + (lines.length - 1)));
			}
		} while (!txSucceeded);

	}

}