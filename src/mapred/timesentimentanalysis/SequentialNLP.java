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


public class SequentialNLP {
//    static StanfordCoreNLP pipeline;
    public static boolean isNumeric(String str)  
    {  
      try  
      {  
        double d = Double.parseDouble(str);  
      }  
      catch(NumberFormatException nfe)  
      {  
        return false;  
      }  
      return true;  
    }
	public static String convert(long unix) {
		Date date = new Date(unix); // *1000 is to convert seconds to milliseconds
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z"); // the format of your date
		sdf.setTimeZone(TimeZone.getTimeZone("GMT-4")); // give a timezone reference for formating (see comment at the bottom
		String formattedDate = sdf.format(date);
		return formattedDate;
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
//		            System.out.println(annotation);
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
    public static void main(String[] args) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader("/Users/xiaokaisun/Desktop/test1"));
		StringBuilder sb = new StringBuilder();
		String line;
        StringBuilder word = new StringBuilder();
		Map<String, Double> output = new HashMap<String, Double>();
		int count = 0;
		double score = 0;
		double ave = 0;
		String day = "";
		while ((line = br.readLine()) != null) {
			count ++;
			StringTokenizer tokenizer = new StringTokenizer(line);
			int i = 0;
			while (tokenizer.hasMoreTokens()) {
				i++;
				String token=tokenizer.nextToken();
				if(i == 3) {
					long timeNum = Long.parseLong(token);
					String realTime = convert(timeNum);
					String[] timeSplit = realTime.split(" ");
					 if (!day.equals(timeSplit[0])) {
						String temp = day;
						 day = timeSplit[0];
						 ave = (double) score/count;
						 score = 0;
						 count = 0;
						 output.put(temp,ave);
//						 System.out.println(ave);
					 }
					 else {
							String[] elements = line.split("\t");
							String tweet = elements[elements.length - 1];
							int everyScore = findSentiment(tweet);
							score += everyScore;
					 }
//					System.out.println(timeSplit[0]);
				}
	       }
//			String[] elements = line.split("\t");
//			String tweet = elements[elements.length - 1];
//			System.out.println(findSentiment(tweet));
			
		}
		output.put(day,(double) score/count);
		System.out.println(output);
//		System.out.println(day);
//		System.out.println((double) score/count);
	}
}