package mapred.hoursentimentanalysis;

import java.io.IOException;
import mapred.job.Optimizedjob;
import mapred.util.SimpleParser;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;

public class Driver {

	public static void main(String args[]) throws Exception {
		SimpleParser parser = new SimpleParser(args);

		String input = parser.get("input");
		String output = parser.get("output");
		
		int reduceJobNumber = parser.getInt("reduceJob");

		getJobFeatureVector(input, output, reduceJobNumber, parser.get("timeslot"));

	}

	private static void getJobFeatureVector(String input, String output, int reduceJobNumber, String timeslot)
			throws IOException, ClassNotFoundException, InterruptedException {
		Configuration conf = new Configuration();
        conf.set("timeslot", timeslot);
		Optimizedjob job = new Optimizedjob(conf, input, output,
				"Compute hour sentiment");

		job.setClasses(SentimentHourMapper.class, SentimentHourReducer.class, null);
		job.setMapOutputClasses(Text.class, Text.class);
		job.setReduceJobs(reduceJobNumber);
		job.run();
	}	
}
