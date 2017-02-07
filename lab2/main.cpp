#include <iostream>
#include <array>
#include <algorithm>
#include <fstream>
#include <optional.hpp>
#include <chrono>

using BoardFormat = std::array<std::array<int, 7>, 7>;

struct Position {int x; int y;};

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

auto createBoard() {
	auto&& file = fileToString(std::ifstream{"p.puzzle"});
	BoardFormat board;
	
	file.erase(
		std::remove(file.begin(), file.end(), '\n'),
		file.end()
	);
	
	std::size_t position = 0;
	for (auto n : file) {
		board[position % 7][position / 7] = n - '0';
		position ++;
	}
	
	return board;
}

auto possibleMoves(const BoardFormat& board, Position pos) {
	std::array<bool,4> moves;
	
	moves[0] = pos.y - 2 > -1 && board[pos.x][pos.y-1] == 1 && board[pos.x][pos.y-2] == 1;
	moves[1] = pos.x + 2 < 8 && board[pos.x+1][pos.y] == 1 && board[pos.x+2][pos.y] == 1;
	moves[2] = pos.y + 2 < 8 && board[pos.x][pos.y+1] == 1 && board[pos.x][pos.y+2] == 1;
	moves[3] = pos.x - 2 > -1 && board[pos.x-1][pos.y] == 1 && board[pos.x-2][pos.y] == 1;
	
	return moves;
}

auto makeMove(BoardFormat board, Position position, int direction){
	if(direction == 0){
		board[position.x][position.y] = 1;
		board[position.x][position.y - 2] = 2;
		board[position.x][position.y - 1] = 2;
	}
	if(direction == 1){
		board[position.x][position.y] = 1;
		board[position.x + 2][position.y] = 2;
		board[position.x + 1][position.y] = 2;
	}
	if(direction == 2){
		board[position.x][position.y] = 1;
		board[position.x][position.y + 2] = 2;
		board[position.x][position.y + 1] = 2;
	}
	if(direction == 3){
		board[position.x][position.y] = 1;
		board[position.x - 2][position.y] = 2;
		board[position.x - 1][position.y] = 2;
	}
	return board;
}

stx::optional<BoardFormat> algo(BoardFormat board){
	int position = 0;
	bool isNotBlock = false;
	int numberOf1 = 0;
	for(auto column : board){
		for(auto n : column){
			Position coordinate{position % 7, position / 7};
			if(n == 1){
				numberOf1 ++;
			}
			if(n == 2){
				auto moves = possibleMoves(board, coordinate);
				
				int direction = 0;
				for(auto validMove: moves){
					if(validMove){
						isNotBlock = true;
						auto nextBoard = makeMove(board,coordinate,direction);
						if (auto result = algo(nextBoard)) {
							return result;
						}
					}
					direction ++;
				}
			}
			position ++;
			
		}
	}
	
	if(numberOf1 == 1){
		std::cout << "canard" << std::endl;
		return board;
	}
	
	if(!isNotBlock){
		return stx::nullopt;
	}
}

int main(int argc, char **argv) {
	using namespace std::chrono;
	auto startTime = high_resolution_clock::now();
	
	algo(createBoard());
	
	auto duration = duration_cast<seconds>(high_resolution_clock::now() - startTime).count();
	
	std::cout << "Time: " << duration << " seconds" << std::endl;
}
