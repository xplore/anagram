package com.moj.anagram;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

public class WordListConsumer {

	public static String wordlisturl = "http://static.abscond.org/wordlist.txt";

	public Map<String, String> wordMap = new ConcurrentHashMap<>();

	public Map<String, List<String>> reverseMap = new ConcurrentHashMap<>();

	public Map<String, List<String>> getAnagramsFor(List<String> words) {

		Map<String, List<String>> results = new HashMap<>();

		words.forEach(word -> {
			char[] sorted = word.toCharArray();
			Arrays.sort(sorted);
			String sortedWord = new String(sorted);
			List<String> anagrams = reverseMap.get(sortedWord);

			if (anagrams == null)
				anagrams = new ArrayList<String>();

			if (anagrams != null)
				anagrams.remove(word);
			results.put(word, anagrams);
		});

		return results;

	}

	public void computeAnagrams() throws InterruptedException {

		final List<String> words = getWordList();

		populateWordMap(words);

	}

	private void populateWordMap(List<String> words) throws InterruptedException {

		ExecutorService executor = Executors.newFixedThreadPool(8);

		words.forEach(word -> {

			executor.submit(new Runnable() {

				@Override
				public void run() {
					char[] word1 = word.replaceAll("[\\s]", "").toCharArray();
					Arrays.sort(word1);
					String sortedString = String.valueOf(word1);
					wordMap.put(word, sortedString);
					if (!reverseMap.containsKey(sortedString)) {
						List<String> s = new ArrayList<String>();
						s.add(word);
						reverseMap.put(sortedString, s);
					} else {
						reverseMap.get(sortedString).add(word);
					}

				}

			});

		});

		executor.shutdown();

		executor.awaitTermination(5, TimeUnit.MINUTES);

		Logger.getGlobal().info("finished populating the word maps");

	}

	private List<String> getWordList() {

		WebClient client = WebClient.create(wordlisturl);

		Mono<ClientResponse> result = client.get().accept(MediaType.TEXT_PLAIN).exchange();

		String[] wordArr = result.flatMap(res -> res.bodyToMono(String.class)).block().split("\n");

		List<String> words = Arrays.asList(wordArr);
		Logger.getGlobal().info(String.format("%s", words.size()));
		return words;
	}

}
