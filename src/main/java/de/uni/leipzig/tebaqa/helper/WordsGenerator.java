package de.uni.leipzig.tebaqa.helper;
import java.io.*;
import java.util.*;
public class WordsGenerator {


        private static Map<String, Set<String>>stopWordMap;
        public WordsGenerator(){
            Properties prop = new Properties();
            try {
                prop.load(new FileInputStream("resources/stopwords.properties"));
                stopWordMap = new HashMap<>();
                for(String lang:prop.stringPropertyNames()){
                    loadStopwords(prop.getProperty(lang)).ifPresent(set -> stopWordMap.put(lang,(Set)set));
                }

            }
            catch (IOException ex) {
                System.out.println("Properties not found");
            }
        }
        private Optional<Set<String>> loadStopwords(String filename){
            FileReader fileReader;
            try {
                Set<String>stopwords=new HashSet<>();
                fileReader = new FileReader(filename);
                BufferedReader bufferedReader = new BufferedReader(fileReader);
                String element;
                while ((element = bufferedReader.readLine()) != null)
                    stopwords.add(element);
                bufferedReader.close();
                return Optional.of(stopwords);
            } catch (FileNotFoundException e) {
                System.out.println("File not found: "+filename);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return Optional.empty();

        }
        public  boolean containsOnlyStopwords(String coOccurence,String lang){
            String[] words = coOccurence.replaceAll("[\\-.?¿!,;]", "").toLowerCase().split("\\s+");
            for(String word:words)
                if(!stopWordMap.get(lang).contains(word))return false;
            return true;
        }
        public static boolean containsAnyStopword(String coOccurence,String lang){
            String[] words = coOccurence.replaceAll("[\\-.?¿!,;]", "").toLowerCase().split("\\s+");
            for(String word:words)
                if(stopWordMap.get(lang).contains(word))return true;
            return false;
    }
        public List<String> generateTokens(String question, String lang){
            Set<String> stopwords = stopWordMap.get(lang);
            String[] words = question.replaceAll("[\\-.?¿!,;]", "").toLowerCase().split("\\s+"); //[\\p{Alnum},\\s#\\-.]
            List<String>keywords = new ArrayList<>();
            for (String word : words)
                if (!stopwords.contains(word))
                    keywords.add(word);
            return keywords;
        }

}
