#include <iostream>
#include <array>
#include <algorithm>
#include <fstream>
#include <optional.hpp>
#include <string_view.hpp>
#include <chrono>
#include <set>
#include <unordered_set>
#include <cstdint>
#include <vector>

template<typename... Args>
constexpr auto make_array(Args&&... args) {
	return std::array<std::common_type_t<Args...>, sizeof...(Args)>{{std::forward<Args>(args)...}};
}

struct Position {
	using type = std::int64_t;
	
	type x;
	type y;
	
	constexpr static auto fromFlat(type n) {
		return Position{n % 7, n / 7};
	}
	
	constexpr type flat() const {
		return x + y * 7;
	}
	
	constexpr auto operator-(const Position& other) const {
		return Position{x - other.x, y - other.y};
	}
	
	constexpr auto operator+(const Position& other) const {
		return Position{x + other.x, y + other.y};
	}
	
	constexpr auto operator*(type multiplier) const {
		return Position{x * multiplier, y * multiplier};
	}
};

constexpr Position middle{3, 3};

struct Move {
	std::uint64_t operation;
};

constexpr Move right{0b0000000'0000000'0000000'0111000'0000000'0000000'0000000'000000000000000ull};
constexpr Move left	{0b0000000'0000000'0000000'0001110'0000000'0000000'0000000'000000000000000ull};
constexpr Move down	{0b0000000'0001000'0001000'0001000'0000000'0000000'0000000'000000000000000ull};
constexpr Move up	{0b0000000'0000000'0000000'0001000'0001000'0001000'0000000'000000000000000ull};

constexpr auto shiftToPosition(Move move, Position position) {
	if (position.flat() < middle.flat()) {
		return Move{move.operation << (middle.flat() - position.flat())};
	} else {
		return Move{move.operation >> (position.flat() - middle.flat())};
	}
}

std::size_t nodes;

std::string output;

struct Board {
	static Board mask;
	
	constexpr auto apply(Move move) const {
		return Board{board ^ move.operation};
	}
	
	constexpr bool operator< (const Board& other) const {
		return board < other.board;
	}
	
	constexpr bool operator== (const Board& other) const {
		return board == other.board;
	}
	
	constexpr auto operator[] (Position position) const {
		auto&& self = (*this);
		
		return self[position.flat()];
	}
	
	constexpr bool operator[] (std::uint64_t position) const {
		return ((1ull << (63ull - position)) & board) != 0ull;
	}
	
	auto print() const {
		auto&& self = (*this);
		std::string out;
		
		for (std::uint64_t i = 0; i<49 ; i++) {
			out += (mask[i] ? (self[i] ? 'o':'-') : (self[i] ? 'x':' '));
			out += ' ';
			if (i % 7 == 6) out += '\n';
		}
		
		return out;
	}
	
	template<typename F>
	constexpr stx::optional<Board> applyPossibleMove(Position pos, F function) {
		auto&& self = (*this);
		
		constexpr auto moves = make_array(up, right, left, down);
		constexpr auto directions = make_array(
			Position{0, 1},
			Position{-1, 0},
			Position{1, 0},
			Position{0, -1}
		);
		
		auto boundCheck = make_array(
			pos.y + 2 < 7,
			pos.x >= 2,
			pos.x + 2 < 7,
			pos.y >= 2
		);
		
		for (std::size_t i = 0 ; i < 4 ; i++) {
			if (boundCheck[i] &&
				self[pos + directions[i]] && self[pos + directions[i] * 2] &&
				mask[pos + directions[i]] && mask[pos + directions[i] * 2]
			) {
				if (auto board = function(pos, moves[i])) return board;
			}
		}
		
		return stx::nullopt;
	}
	
	std::uint64_t board = 0;
};

Board Board::mask;

namespace std {

template<>
struct hash<Board> {
	using argument_type = Board;
	using result_type = size_t;
	result_type operator()(const argument_type& board) const {
		return hash<std::uint64_t>{}(board.board);
	}
};

}

std::unordered_set<Board> seenBoards;

std::vector<Position> pegToCheck;

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

auto createBoard(stx::string_view path) {
	auto&& file = fileToString(std::ifstream{path.data()});
	Board board;
	
	file.erase(
		std::remove(file.begin(), file.end(), '\n'),
		file.end()
	);
	
	std::size_t position = 0;
	
	for (auto n : file) {
		auto number = n - '0';
		
		board.board += ((1ull << (63 - position)) * ((1 - (number - 1)) % 2));
		Board::mask.board += ((1ull << (63 - position)) * (n - '0' > 0));
		if (n > 0) pegToCheck.emplace_back(Position::fromFlat(position));
		
		position++;
	}
	
	return board;
}

stx::optional<Board> algo(Board board) {
	nodes++;
	std::uint32_t numberOf1 = 0;
	
	auto iterator = seenBoards.find(board);
	
	if (iterator != seenBoards.end()) {
		return stx::nullopt;
	}
	
	seenBoards.emplace(board);
	
	for (auto&& position : pegToCheck) {
		auto&& n = board[position.flat()];
		
		numberOf1 += n;
		
		if (!n && Board::mask[position]) {
			auto applyMove = [&](auto pos, auto move){
				return algo(board.apply(shiftToPosition(move, pos)));
			};
			
			if (auto solution = board.applyPossibleMove(position, applyMove)) {
				output = board.print() + '\n' + output;
				return solution;
			}
		}
	}
	
	if (numberOf1 == 1) {
		return board;
	}
	
	return stx::nullopt;
}

int main(int argc, char **argv) {
	using namespace std::chrono;
	
	if (argc != 2) {
		std::cout << "Usage: peg file.puzzle" << std::endl;
		return 0;
	}
	
	Board board = createBoard(argv[1]);
	
	auto startTime = high_resolution_clock::now();
	auto solution = algo(board);
	auto duration = duration_cast<microseconds>(high_resolution_clock::now() - startTime).count();
	
	std::cout << output;
	
	std::cout << "--------------";
	std::cout << std::endl;
	
	
	std::cout << nodes << " nodes explored." << std::endl;
	std::cout << std::endl;
	if (solution) {
		std::cout << solution->print();
		std::cout << std::endl;
	} else {
		std::cout << "No solution" << std::endl;
	}
	
	std::cout << "Temps d'execution : " << (duration > 10000 ? duration / 1000.f : duration) << (duration > 10000 ? " millisecondes\n" : " microsecondes.\n") << std::endl;

}
