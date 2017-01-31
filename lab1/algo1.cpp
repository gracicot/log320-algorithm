#include <iostream>

#include <string_view.hpp>
#include <string>
#include <vector>
#include <fstream>
#include <algorithm>
#include <sstream>
#include <numeric>
#include <chrono>
#include <cctype>

bool isAnagram(stx::string_view string1, std::string string2) {
	string2.erase(
		std::remove_if(string2.begin(), string2.end(), [](auto&& c){ return std::isspace(c); }),
		string2.end()
	);
	
	for (auto&& c1 : string1) {
		if (!std::isspace(c1)) {
			auto characterFound = std::find(string2.begin(), string2.end(), c1);
			
			if (characterFound != string2.end()) {
				string2.erase(characterFound);
			} else {
				return false;
			}
		}
	}
	
	return string2.empty();
}

std::string fileToString(std::ifstream in) {
	std::string content;
	
	if (in) {
		in.seekg(0, std::ios::end);
		content.resize(in.tellg());
		in.seekg(0, std::ios::beg);
		in.read(&content[0], content.size());
		in.close();
	}
	
	return content;
}

template<typename T>
auto parseWords(T stream) {
	std::vector<std::string> words;
	
	if (stream) {
		std::string word;
		while(std::getline(stream, word)) {
			words.emplace_back(std::move(word));
		}
	}
	
	return words;
}

int main(int argc, char** argv) {
	using namespace std::chrono;

	if (argc != 3) {
		std::cout << "Usage: algo1 words dict" << std::endl;
		return 1;
	}

	auto wordsFile = fileToString(std::ifstream{ argv[1] });
	auto dictFile = fileToString(std::ifstream{ argv[2] });

	high_resolution_clock::time_point startTime = high_resolution_clock::now();

	auto words = parseWords(std::stringstream{ wordsFile });
	auto dict = parseWords(std::stringstream{ dictFile });

	std::vector<std::pair<stx::string_view, std::size_t>> anagramPerWords;

	for (auto&& word : words) {
		anagramPerWords.emplace_back(word, std::count_if(dict.begin(), dict.end(), [&word](auto&& anagram) { return isAnagram(word, anagram); }));
	}

	auto duration = duration_cast<microseconds>(high_resolution_clock::now() - startTime).count();

	for (auto&& a : anagramPerWords) {
		std::cout << "Il y a " << a.second << " anagrammes du mot " << a.first << '\n';
	}

	std::cout << "Il y a un total de "
		<< std::accumulate(anagramPerWords.begin(), anagramPerWords.end(), 0, [](auto&& a, auto&& b) { return a + b.second; }) << " anagrammes.\n"
		<< "Temps d'execution : " << (duration > 10000 ? duration / 1000.f : duration) << (duration > 10000 ? " millisecondes\n" : " microsecondes.\n")
		<< std::flush;
}
