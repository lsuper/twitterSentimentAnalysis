 import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.util.Properties;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.CoreMap;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TimeZone;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.util.*;

 public class TimeSentimentAnalysis extends Configured implements Tool {

   public static class Map extends MapReduceBase implements Mapper<LongWritable, Text, Text, DoubleWritable> {

     static enum Counters { INPUT_WORDS }

//     private final static DoubleWritable one = new DoubleWritable(1);
     private Text word = new Text();

     private boolean caseSensitive = true;
     private Set<String> patternsToSkip = new HashSet<String>();

     private long numRecords = 0;
     private String inputFile;

     public void configure(JobConf job) {
       caseSensitive = job.getBoolean("wordcount.case.sensitive", true);
       inputFile = job.get("map.input.file");

       if (job.getBoolean("wordcount.skip.patterns", false)) {
         Path[] patternsFiles = new Path[0];
         try {
           patternsFiles = DistributedCache.getLocalCacheFiles(job);
         } catch (IOException ioe) {
           System.err.println("Caught exception while getting cached files: " + StringUtils.stringifyException(ioe));
         }
         for (Path patternsFile : patternsFiles) {
           parseSkipFile(patternsFile);
         }
       }
     }

     private void parseSkipFile(Path patternsFile) {
       try {
         BufferedReader fis = new BufferedReader(new FileReader(patternsFile.toString()));
         String pattern = null;
         while ((pattern = fis.readLine()) != null) {
           patternsToSkip.add(pattern);
         }
       } catch (IOException ioe) {
         System.err.println("Caught exception while parsing the cached file '" + patternsFile + "' : " + StringUtils.stringifyException(ioe));
       }
     }
     public static int findSentiment(String tweet) {

         try {
         	 	Properties props = new Properties();
         	    props.setProperty("annotators", "tokenize, ssplit, parse, sentiment");
         	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
 		        	int mainSentiment = 0;
 		        if (tweet != null && tweet.length() > 0) {
 		            int longest = 0;
 		            Annotation annotation = pipeline.process(tweet);
// 		            System.out.println(annotation);
 		            for (CoreMap sentence : annotation
 		                    .get(CoreAnnotations.SentencesAnnotation.class)) {
 		                Tree tree = sentence
 		                        .get(SentimentCoreAnnotations.AnnotatedTree.class);
 		                int sentiment = RNNCoreAnnotations.getPredictedClass(tree);
 		                String partText = sentence.toString();
 		                if (partText.length() > longest) {
 		                    mainSentiment = sentiment;
 		                    longest = partText.length();
 		                }
 		
 		            }
 		        }
 		        return mainSentiment;
         } catch(NullPointerException e) {
         	System.out.println("nullpointerexception");
         	return 0;
         }
 		
     }
 	public static String convert(long unix) {
		Date date = new Date(unix); // *1000 is to convert seconds to milliseconds
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z"); // the format of your date
		sdf.setTimeZone(TimeZone.getTimeZone("GMT-4")); // give a timezone reference for formating (see comment at the bottom
		String formattedDate = sdf.format(date);
		return formattedDate;
	}

     public void map(LongWritable key, Text value, OutputCollector<Text, DoubleWritable> output, Reporter reporter) throws IOException {
       String line = (caseSensitive) ? value.toString() : value.toString().toLowerCase();

       for (String pattern : patternsToSkip) {
         line = line.replaceAll(pattern, "");
       }

//       StringTokenizer tokenizer = new StringTokenizer(line);
//       while (tokenizer.hasMoreTokens()) {
//         word.set(tokenizer.nextToken());
//         output.collect(word, one);
//         reporter.incrCounter(Counters.INPUT_WORDS, 1);
//       }
       StringTokenizer tokenizer = new StringTokenizer(line);
		int i = 0;
		int whitePos = 0;
		while (tokenizer.hasMoreTokens()) {
			i++;
			String token=tokenizer.nextToken();
			if(i == 3) {
				long timeNum = Long.parseLong(token);
				String realTime = convert(timeNum);
				String[] timeSplit = realTime.split(" ");
				System.out.println(timeSplit[0]);
				word.set(timeSplit[0]);
				String[] elements = line.split("\t");
				String tweet = elements[elements.length - 1];
			    DoubleWritable score = new DoubleWritable(findSentiment(tweet));
			    output.collect(word, score);
			}
      
		}
       if ((++numRecords % 100) == 0) {
         reporter.setStatus("Finished processing " + numRecords + " records " + "from the input file: " + inputFile);
       }
     }
   }

   public static class Reduce extends MapReduceBase implements Reducer<Text, DoubleWritable, Text, DoubleWritable> {
     public void reduce(Text key, Iterator<DoubleWritable> values, OutputCollector<Text, DoubleWritable> output, Reporter reporter) throws IOException {
       int sum = 0;
       int n = 0;
       while (values.hasNext()) {
    	  n++;
        sum += values.next().get();
       }
       double ave = (double) sum/n;
       output.collect(key, new DoubleWritable(ave));
     }
   }

   public int run(String[] args) throws Exception {
     JobConf conf = new JobConf(getConf(), TimeSentimentAnalysis.class);
     conf.setJobName("wordcount");

     conf.setOutputKeyClass(Text.class);
     conf.setOutputValueClass(DoubleWritable.class);

     conf.setMapperClass(Map.class);
     conf.setCombinerClass(Reduce.class);
     conf.setReducerClass(Reduce.class);

     conf.setInputFormat(TextInputFormat.class);
     conf.setOutputFormat(TextOutputFormat.class);

     List<String> other_args = new ArrayList<String>();
     for (int i=0; i < args.length; ++i) {
       if ("-skip".equals(args[i])) {
         DistributedCache.addCacheFile(new Path(args[++i]).toUri(), conf);
         conf.setBoolean("wordcount.skip.patterns", true);
       } else {
         other_args.add(args[i]);
       }
     }

     FileInputFormat.setInputPaths(conf, new Path(other_args.get(0)));
     FileOutputFormat.setOutputPath(conf, new Path(other_args.get(1)));

     JobClient.runJob(conf);
     return 0;
   }

   public static void main(String[] args) throws Exception {

     int res = ToolRunner.run(new Configuration(), new TimeSentimentAnalysis(), args);
     System.exit(res);
   }
 }
