package com.moj.anagram;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AnagramController {

	WordListConsumer consumer = new WordListConsumer();

	@PostConstruct
	public void postConstruct() throws InterruptedException {
		Logger.getGlobal().info("start");

		consumer.computeAnagrams();

		Logger.getGlobal().info("end");
	}

	@RequestMapping(value = "/{wordString}", method = RequestMethod.GET, produces = "application/json")
	@ResponseBody()
	public Map<String, List<String>> findAnagram(@PathVariable String wordString) {

		String[] wordsArr = wordString.split(",");
		List<String> words = Arrays.asList(wordsArr);
		Map<String, List<String>> result = consumer.getAnagramsFor(words);
		return result;
	}

}
