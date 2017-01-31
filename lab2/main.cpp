#include <iostream>
#include <array>

using BoardFormat = std::array<std::array<int, 7>, 7>;

struct Position {int x; int y;};

int createBoard() {
    std::cout << "Hello, world!" << std::endl;
	
    return 0;
}

auto possibleMoves(BoardFormat board, Position pos) {
    std::array<bool,4> moves;
	
	moves[0] = pos.y - 2 > -1 && board[pos.x][pos.y-1] == 1 && board[pos.x][pos.y-2] == 1;
	moves[1] = pos.x + 2 < 8 && board[pos.x+1][pos.y] == 1 && board[pos.x+2][pos.y] == 1;
	moves[2] = pos.y + 2 < 8 && board[pos.x][pos.y+1] == 1 && board[pos.x][pos.y+2] == 1;
	moves[3] = pos.x - 2 > -1 && board[pos.x-1][pos.y] == 1 && board[pos.x-2][pos.y] == 1;
	
    return moves;
}

int main(int argc, char **argv) {
    std::cout << "Hello, world!" << std::endl;
	
    return 0;
}
