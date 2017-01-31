#include <iostream>
#include <string>
#include <array>
#include <vector>
#include <string_view.hpp>
#include <chrono>
#include <algorithm>
#include <numeric>
#include <fstream>
#include <cstdint>
#include <unordered_map>

#include <boost/container/flat_map.hpp>

namespace std {
	template<typename T, size_t N>
	struct hash<array<T, N>> {
		using argument_type = array<T, N>;
		using result_type = size_t;

		result_type operator()(const argument_type& array) const {
			hash<T> hasher;
			result_type result = 0;

			for (auto&& element : array) {
				result *= 31 + hasher(element);
			}

			return result;
		}
	};
}

template<typename Stream>
std::string fileToString(Stream&& in) {
	std::string content;

	if (in) {
		in.seekg(0, in.end);
		content.resize(in.tellg());
		in.seekg(0, in.beg);
		in.read(&content[0], content.size());
		in.close();
	}

	return content;
}

struct Algo2 {
	using algo_result = std::vector<std::pair<stx::string_view, std::size_t>>;

	using number_type = std::uint64_t;

	constexpr static std::size_t subnumber_size = 2;
	constexpr static std::size_t letter_amount = 36;

	using compare_array = std::array<number_type, (letter_amount * subnumber_size) / sizeof(number_type)>;

	using small_map = boost::container::flat_map<compare_array, std::size_t>;
	using large_map = std::unordered_map<compare_array, std::size_t>;
	
	template<typename T>
	static void iterateWord(stx::string_view words, T&& function) {
		auto wordBegin = words.begin();
		for (auto wordEnd = words.begin(); wordEnd != words.end(); wordEnd++) {
			if (*wordEnd == '\n') {
				function(stx::string_view{&*wordBegin, static_cast<std::size_t>(std::distance(wordBegin, wordEnd))});
				wordBegin = wordEnd + 1;
			}
		}
	}
	
	void loadFiles(stx::string_view wordPath, stx::string_view anagramsPath) {
		words = fileToString(std::ifstream{ wordPath.data() });
		anagrams = fileToString(std::ifstream{ anagramsPath.data() });
	}

	void preprocess() {
		words.erase(
			std::remove(words.begin(), words.end(), ' '),
			words.end()
		);

		anagrams.erase(
			std::remove(anagrams.begin(), anagrams.end(), ' '),
			anagrams.end()
		);
	}

	auto countCharacter(stx::string_view str) const {
		compare_array charCounterStr{};

		constexpr auto subnumbers = sizeof(number_type) / subnumber_size;

		static_assert(subnumbers * charCounterStr.size() >= letter_amount, "the compare_array type is too small to hold all values.");

		for (const auto& c : str) {
			std::size_t which = (c - 'a' + 10) - 39 * ((c - 'a') >> 7);

			charCounterStr[which / subnumbers] += 1ull << (which % subnumbers) * subnumbers;
		}

		return charCounterStr;
	}

	template<typename T>
	auto algo() const {
		algo_result anagramPerWords;

		T compareTable;

		compareTable.reserve(words.size()); // approximate the number of anagrams very roughly
		anagramPerWords.reserve(words.size()); // approximate the number of words very roughly

		iterateWord(anagrams, [&](const auto& anagram) {
			auto result = compareTable.emplace(countCharacter(anagram), 1);

			// increment if not inserted.
			result.first->second += !result.second;
		});

		iterateWord(words, [&](const auto& word) {
			anagramPerWords.emplace_back(word, compareTable[countCharacter(word)]);
		});

		return anagramPerWords;
	}

	void run() const {
		using namespace std::chrono;

		auto startTime = high_resolution_clock::now();
		auto result = anagrams.size() > 50000000 ? algo<large_map>() : algo<small_map>();
		auto duration = duration_cast<microseconds>(high_resolution_clock::now() - startTime).count();
		
		printResult(std::move(result), duration);
	}

	template<typename T>
	static void printResult(T anagramPerWords, double duration) {
		for (auto a : anagramPerWords) {
			std::cout << "Il y a " << a.second << " anagrammes du mot " << a.first << '\n';
		}

		std::cout << "Il y a un total de "
			<< std::accumulate(anagramPerWords.begin(), anagramPerWords.end(), 0, [](auto&& a, auto&& b) { return a + b.second; }) << " anagrammes.\n"
			<< "Temps d'execution : " << (duration > 10000 ? duration / 1000.f : duration) << (duration > 10000 ? " millisecondes\n" : " microsecondes.\n")
			<< std::flush;
	}

private:
	std::string words;
	std::string anagrams;
};

int main(int argc, char** argv) {
	if (argc != 3) {
		std::cout << "Usage: algo2 words dict" << std::endl;
		return 1;
	}

	Algo2 algo;

	algo.loadFiles(argv[1], argv[2]);
	algo.preprocess();
	algo.run();
}
