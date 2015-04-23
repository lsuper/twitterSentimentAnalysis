package mapred.hoursentimentanalysis;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

public class SentimentHourMapper extends Mapper<LongWritable, Text, Text, Text> {
	
	Map<String, Coordinate> cityMap;
	Map<String, String> cityTimeZone;
	String timeslot;
	public static String getDate(long unix, String timezone) {
		Date date = new Date(unix); // *1000 is to convert seconds to milliseconds
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z"); // the format of your date
		sdf.setTimeZone(TimeZone.getTimeZone(timezone)); // give a timezone reference for formating (see comment at the bottom
		String formattedDate = sdf.format(date);
		return formattedDate;
	}
	
	@Override
	protected void setup(Context context) {
		cityMap = new HashMap<String, Coordinate>();
		cityMap.put("Pittsburgh", new Coordinate(-80.0, 40.44));
		cityMap.put("San Francisco", new Coordinate(-122.44, 37.71));
		cityMap.put("New York", new Coordinate(-73.97, 40.75));
		cityMap.put("Houston", new Coordinate(-95.36, 29.75));
		cityMap.put("Chicago", new Coordinate(-87.78, 41.83));
		cityMap.put("Miami", new Coordinate(-80.24, 25.93));
		cityMap.put("London", new Coordinate(-0.085, 51.5));
		
		cityTimeZone = new HashMap<String, String>();
		cityTimeZone.put("Pittsburgh", "GMT-4");
		cityTimeZone.put("San Francisco", "GMT-7");
		cityTimeZone.put("New York", "GMT-4");
		cityTimeZone.put("Houston", "GMT-5");
		cityTimeZone.put("Chicago", "GMT-5");
		cityTimeZone.put("Miami", "GMT-4");
		cityTimeZone.put("London", "GMT+1");
		
		timeslot = context.getConfiguration().get("timeslot", "hour") ;
	}

	@Override
	protected void map(LongWritable key, Text value, Context context)
			throws IOException, InterruptedException {
		String line = value.toString();
		String[] fields = line.split("\t");
		//System.out.println(line);
		String tweet = fields[fields.length - 1];
		String coordStr = fields[4];
		String city;
		String time;
		if (!coordStr.isEmpty()) {
			Coordinate coord = new Coordinate(Double.parseDouble(coordStr.split(",")[0]), Double.parseDouble(coordStr.split(",")[1]));
			city = coord.getClosestCityName(this.cityMap);
			if (timeslot.equals("date")){
				time = getDate(Long.parseLong(fields[2]), this.cityTimeZone.get(city)).substring(0, 10);
			}
			else
			{
				//timeslot == hour
				time = getDate(Long.parseLong(fields[2]), this.cityTimeZone.get(city)).substring(11, 13);
			}
			
			context.write(new Text(time), new Text(tweet));
		}
	}
}
