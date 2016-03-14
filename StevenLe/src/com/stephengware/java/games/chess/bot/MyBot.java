package com.stephengware.java.games.chess.bot;

import java.util.HashMap;
import java.util.Iterator;

import com.stephengware.java.games.chess.bot.Bot;
import com.stephengware.java.games.chess.state.State;
import com.stephengware.java.games.chess.state.Board;
import com.stephengware.java.games.chess.state.Piece;
import com.stephengware.java.games.chess.state.Player;
/**
 * An awesome chessbot which uses iterative deepening search to look ahead a maximum of 5 moves 
 * also take into account pieces' positions, potential movements and potential capture opportunities
 * 
 * @author Hung L. Le
 */
public class MyBot extends Bot {
	// instance variables
	HashMap<String,Integer> previousStatesMap; // the Hashmap that keeps track of the number of previous occurrence of a state
	State rootState ; 	// the state at the root of the tree
	Player mySide ; 	// the variable that keeps track of the current side
	int depthLimit;		// the depth limit, used for iterative deepening
	boolean stopSearching;	// set to true when searchLimit is reached
	boolean captureTheKing;		// set to true when it's time to capture the king
	double totalExtraFactor;	// the total of factors considered other than material score 
	double bestCapture;	// the best capture value can be achieved, used for capture prediction 


	/**
	 * Constructs a new chess bot named "HungLe"
	 */
	public MyBot() {
		
		super("hlle");
		
		// set up needed variables
		captureTheKing = false;
		mySide = null;
		previousStatesMap = new HashMap<String, Integer>();
		
	}
	
	@Override
	/**
	 * The main method to choose a move
	 * 
	 * @param state - the current state of the game
	 * @return the selected next state
	 */
	protected State chooseMove(State state) {
		
			
		// when new game begins, reset variables
		if ( mySide != state.player)
		{
			if ( mySide != null)
			{
				captureTheKing = false;
				previousStatesMap.clear();
			}
		}
		
		// Record opponent's selected state for draw state checking
		if ( state.toString() != "")
		{
			String stateName = state.toString().substring(state.toString().lastIndexOf(" "));
			
			if ( !this.previousStatesMap.containsKey(stateName))
			{
				this.previousStatesMap.put(stateName, 1);
			}
			else
			{
				this.previousStatesMap.put(stateName, 2);
			}
		}
		
		
		
		/**
		 * Begin state selection algorithm from HERE
		 */
		this.rootState = state; // save the starting state
		mySide = state.player; 	// keep track of what side I am: BLACK or WHITE
		depthLimit = 2; // starts at depth 2
		this.stopSearching = false; // searching will halt when this flag is set
		
		/**
		 *  create a GameTree object with the current state as a parameter 
		 *  GameTree is basically a state, only with much more relevant information
		 */
		GameTree root = new GameTree(state);
		
		GameTree chosenNode = root; 	// the node that holds the state to be chosen 
		
		// Loop infinitely until a result is found
		while ( true )
		{
			/**
			 *  THE EVALUATED VALUE IS ALWAYS LARGER AS THIS BOT BETTER OFF
			 *  therefore, this bot always start with findMax
			 */
			findMax(root, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,0);
			
			
			/**
			 * After findMax returns, call method reorderGameTree() on this tree,
			 * to sort its children list in descending order of evaluated value
			 * ==> Improve pruning in the next iteration
			 * 
			 * Also take the node with the highest value as a return
			 * this is the best state found in this iteration
			 */
			GameTree maxNode = root.reorderGameTree(this.previousStatesMap);
			


			// failed search: stopSearching is set when search limit is reached
			if ( this.stopSearching)
			{
				break;
			}
			
			// successful search
			else
			{
				// update the chosenNode
				chosenNode = maxNode;
			}
			
			
			// if depthLimit = 5, stop the loop, this bot is designed to reach depth 5 only
			if ( depthLimit ==  5 )
			{
				break;
			}

			
			// update the depthLimit for iterative deepening
			// the iterations will go with depth 2, then 4, then 5
			depthLimit += depthLimit < 4 ? 2 : 1;
		}
		
		
		// record the chosen state for draw game checking
		if ( !this.previousStatesMap.containsKey(chosenNode.stateName))
		{
			this.previousStatesMap.put(chosenNode.stateName, 1);
		}
		else
		{
			this.previousStatesMap.put(chosenNode.stateName, 2);
		}
		
		// return the chosen next state
		return chosenNode.getState();
		
	}// end of chooseMove
	

	/**
	 * Given a {@link GameTree} node, expand its children (if any) to find the
	 * node with the highest minimum utility value.
	 * 
	 * @param tree the node whose children need to be expanded
	 * @param alpha the highest utility value discovered so far in this branch of the tree (i.e. best for X)
	 * @param beta the lowest utility value discovered so far in this branch of the tree (i.e. best for O)
	 * @return the utility value of the node with the highest minimum utility
	 */
	private double findMax(GameTree tree, double alpha, double beta, int depth) {
		
		
		// stop searching when stopSearching flag is set
		if ( this.stopSearching == true)
		{
			return Double.NEGATIVE_INFINITY;
		}
		
		// Make sure the number of expanded states doesn't exceed 500,000
		else if ( this.rootState.countDescendants() > 499000)
		{
			// set the stopSearching flag 
			this.stopSearching = true;
			return Double.NEGATIVE_INFINITY;
		}

		// return when depthLimit is reached
		if(depth == depthLimit)
		{	
			return evaluate(tree.getState(), true);
		}
		
		// If depth limit is not reached, then we need to expand all of the tree's
		// children and find the one with the highest minimum utility value.
		// Start with the lowest possible number, double#NEGATIVE_INFINITY and
		// work our way up from there.
		double max = Double.NEGATIVE_INFINITY;
		
		// If the tree still has more children
		while(tree.hasNextChild()) {
			
			// get the next unexplored child node with GameTree#getNextChild().
			GameTree child = tree.getNextChild();

			// Find the lowest possible utility value the child node can have.
			child.value = findMin(child, alpha, beta, depth+1);


			// Update 'max' based on this new information.  'max' should always hold the
			// largest value we have discovered so far.
			max = Math.max(max, child.value);
			
			// The parameter 'beta' holds the lowest utility value that has been
			// discovered so far in this branch of the game tree.  We are currently
			// looking for the child with the highest value, but if we find something
			// that is greater than or equal to beta, there is no reason to bother
			// checking more children nodes because a better move must already exist
			// somewhere else that has already been explored.
			if(max >= beta) // pruned
			{
				return max + 1.0; // IMPORTANT: return a higher value to ensure that findMin won't choose pruned branch
			}		
			
			// Update alpha to be the highest value discovered so far.
			alpha = Math.max(alpha, max);
		}
		return max;
	}// end of findMax
	
	
	
	
	/**
	 * Given a {@link GameTree} node, expand its children (if any) to find the
	 * node with the lowest maximum utility value.
	 * 
	 * @param tree the node whose children need to be expanded
	 * @param alpha the highest utility value discovered so far in this branch of the tree (i.e. best for X)
	 * @param beta the lowest utility value discovered so far in this branch of the tree (i.e. best for O)
	 * @return the utility value of the node with the lowest maximum utility
	 */
	private double findMin(GameTree tree, double alpha, double beta, int depth) {
		// This method is simply the opposite of #findMax.
		
		
		// stop searching when stopSearching flag is set
		if ( this.stopSearching == true)
		{
			return Double.NEGATIVE_INFINITY;
		}	
				
		// Make sure the number of expanded states doesn't exceed 500,000
		if ( this.rootState.countDescendants() > 499000)
		{
			// set the stopSearching flag 
			this.stopSearching = true;
			return Double.NEGATIVE_INFINITY;
		}
		
		
		// checks whether this immediate next state is a game-over state
		if (depth == 1)
		{
			if ( tree.getState().over)
			{
				if ( tree.getState().check)
					// opponent loses by check mate = GOOD
					return Double.POSITIVE_INFINITY;
				else
					// draw by stale mate = BAD
					return Double.NEGATIVE_INFINITY; 
			}
			
			// draw by threefold = BAD
			if ( tree.checkDrawState(tree, previousStatesMap))
			{
				return Double.NEGATIVE_INFINITY;
			}
			
		}
		
		
		// return when depthLimit is reached
		if(depth == depthLimit)
		{
			return evaluate(tree.getState(), true);
		}
		
		double min = Double.POSITIVE_INFINITY;
		while(tree.hasNextChild()) {
			GameTree child = tree.getNextChild();
			

			child.value = findMax(child, alpha, beta, depth+1);
			
			
			min = Math.min(min, child.value);
			// The parameter 'alpha' holds the highest utility value that has been
			// discovered so far in this branch of the game tree.  We are currently
			// looking for the child with the lowest value, but if we find something
			// that is less than or equal to alpha, there is no reason to bother checking
			// more children nodes because a better move must already exist somewhere
			// else that has already been explored.
			if(min <= alpha)
			{
				return min - 1.0; // return a lower value than min to ensure that max won't choose pruned branches 
			}
			// Update beta to be the lowest value discovered so far.
			beta = Math.min(beta, min);
		}
		return min;
	}// end of findMin
	
	
	/**
	 * Evaluate a game state
	 * The better the state is for THIS BOT, the larger the evaluated value, and vice versa
	 * 
	 * @param state - the state to be evaluated
	 * @param withExtraFactor - false : return only material score, true : return material score plus additional factors
	 * @return the evaluated value for the said state 
	 */
	private double evaluate(State state, boolean withExtraFactor)
	{
		
		// an iterator that holds the pieces currently present on the board 
		Iterator<Piece> currentPieces = state.board.iterator();
				
		// the value to be returned
		double value = 0.0;
		
		// the variable that holds the sum of all extra factors other than material score
		// value of this variable will be updated by the getPieceValue() method if the withExtraFactor flag is set
		this.totalExtraFactor = 0.0;
		
		// holds the value of the best piece can be captured by the player is this state
		this.bestCapture = 0.0;
		
		// the piece to be considered
		Piece piece = null;
		
		// loop through all the pieces on the board, friends or foes
		while ( currentPieces.hasNext() )
		{
			piece = currentPieces.next();
			
			// call method getPieceValue() to evaluate the value of a given piece
			
			// ADD the value of MY pieces to the total value
			if ( piece.player.equals(this.mySide))
				value += this.getPieceValue(piece, state.turn, state, withExtraFactor);
			
			// SUBTRACT the value of OPPONENT's pieces from the total value
			else
				value -= this.getPieceValue(piece, state.turn, state, withExtraFactor);
		}
		
		// return the piece value plus the castling Factor 
		//( the beneficial factor where castling occurs for friendly King )
		double castlingFactor = state.player.equals(mySide) ? this.getCastlingFactor(state) : 0.0 ;
		
		if ( withExtraFactor )
		{
			this.totalExtraFactor += castlingFactor;
		}
		
		// extra factor cannot be too high as to avoid the loss of a piece just for a better position
		// ==> make sure it is no larger than 10.0, which is the value of a Pawn
		while(Math.abs(this.totalExtraFactor) >= 10.0)
			this.totalExtraFactor *= 0.5;
		
		// the capture value is POSITIVE if it's my turn and NEGATIVE if it's opponent's turn
		// the capture value is updated by the findBestCapture method called by getPieceValue
		this.bestCapture = state.player.equals(this.mySide) ? this.bestCapture : -this.bestCapture ;
		
		
		// return material score plus all other factor
		return value + this.totalExtraFactor+ this.bestCapture;	
	}// end of evaluate
	
	
	/**
	 * Evaluate value of the given piece, including its material score and
	 * a number of other factors: positioning factor, potential move & capture factor...
	 * 
	 * @param piece - the piece to be evaluated
	 * @param turn  - the turn number, this affects how different factor is adjusted
	 * @param state - the current state to be considered
	 * @param withExtraFactor - false : return only material score, true : return material score plus additional factors
	 * @return the value of the piece
	 */
	private double getPieceValue( Piece piece, int turn, State state, boolean withExtraFactor)
	{
		double value = 0.0;
		
		// the name of the piece
		String pieceName = "";
		
		// adjust the rank so relative rank are the same for Black and White pieces
		int adjustedRank = piece.player.toString().equals("WHITE")? piece.rank : (7 - piece.rank);
		
		switch (piece.toString())
		{
			case "P":
				value = 10.0;
				pieceName = "Pawn";
				break;
			case "R":
				value = 50.0;
				pieceName = "Rook";
				break;
			case "N":
				value = 30.0;
				pieceName = "Knight";
				break;
			case "B":
				value = 30.0;
				pieceName = "Bishop";
				break;
			case "Q":
				value = 90.0;
				pieceName = "Queen";
				break;
			case "K":
				pieceName = "King";
				value = 100.0;
				break;
		}
		
		
		/**
		 *  If withExtraFactor flag is set
		 *  add all the calculated extra factor to the totalExtraFactor sum
		 *  
		 *  positionFactor: consider the position of the piece
		 *  potentialFactor: consider what the piece can do in the next turn
		 *  openingFactor: encourages the piece to come into the game instead of idling
		 */
		
		if ( withExtraFactor)
		{
			// ADD the extra factors of MY pieces to the total extra factors
			if ( piece.player.equals(this.mySide))
			{
				double posFactor = this.getPositionFactor(piece,pieceName, piece.file, adjustedRank, turn);
				double pttFactor = this.getPotentialFactor(state, piece, pieceName) ;
				double oFactor = this.getOpeningFactor(state, piece, value);

				this.totalExtraFactor += (posFactor + pttFactor + oFactor);
			}
			
			// In OPPONENT's turn, assess his potential and subtract it from totalExtraFactor,
			// as the more potential my opponent has, the less desirable it is for me
			else if ( !state.player.equals(this.mySide))
			{
				double pttFactor = this.getPotentialFactor(state, piece, pieceName) ;
				this.totalExtraFactor -= pttFactor;
			}
				
				
		}
		 
		return value;
	} // end of getPieceValue
	
	
	
	
	// OVERLOADED getPieceValue method, returns only the material score of the piece
	private double getPieceValue( Piece piece)
	{
		return this.getPieceValue(piece, 0, null, false);
	} // end of overloaded getPieceValue

	
	
	/**
	 * return the value with respect to a piece's position
	 * 
	 * @param piece
	 * @param pieceName
	 * @param file
	 * @param rank
	 * @param turn
	 * @return the value with respect to a piece's position
	 */
	private double getPositionFactor(Piece piece, String pieceName, int file, int rank, int turn)
	{
		// positionFactor consists of rank and file factors
		// evaluate only my pieces to reduce time complexity
		if ( piece.player.equals(this.mySide))
			return getRankFactor(pieceName,rank,turn) + getFileFactor(pieceName,file);
		else
			return 0.0;
	}
	
	
	/**
	 * return the value of a piece with respect to its rank
	 * range: 4.0 - 8.0
	 * @param piece
	 * @param rank
	 * @param turn
	 * @return the value of a piece with respect to its rank
	 */
	private double getRankFactor(String piece, int rank, int turn)
	{
		double rankFactor = 0.0;
		
		// For the king, position in lower ranks is preferred
		if ( piece.equals("King"))
		{
			if ( turn < 50 )
				rankFactor = -(double)rank/3.0;
		}
		
		// Pawns usually better off moving forward
		if ( piece.equals("Pawn"))
		{
			if ( turn < 30)
				rankFactor += rank <= 4 ? ((double) rank) : 4.0;  
			// toward the end of the game, move forward more aggressively
			else
			{
				rankFactor += (double) rank;
			}
		}
		
		// For other pieces
		else
		{
			// position in the middle field is preferred
			if ( turn < 35)
				rankFactor += rank <= 3 ? ((double) rank) : 3.0;  
		}
		
		// return rankFactor, as the game comes to the end, rankFactor becomes directly proportional with the turn
		return rankFactor * (turn > 40 ? (double)turn/20.0: 1.0 ) ;
	}
	
	/**
	 * return the value of a piece with respect to its file
	 * 
	 * @param piece
	 * @param file
	 * @return the value of a piece with respect to its file
	 */
	private double getFileFactor(String piece, int file)
	{
		double fileFactor = 0.0;
		
		
		// Cornered knights and bishops are undesired
		if ( piece.equals("Knight") || piece.equals("Bishop"))
		{
			if ( file == 0 || file == 7)
			{
				fileFactor = -2.0;
			}
		}
				
		return fileFactor;
	}
	
	
	
	/**
	 * meant to encourage all the pieces to participate in the game
	 * range : -2.0 - -5.0
	 * @param state
	 * @param piece
	 * @param value
	 * @return
	 */
	private double getOpeningFactor ( State state, Piece piece, double value)
	{
		double oFactor = 0.0;
		
		
		if ( !piece.equals("K") && piece.player.equals(mySide))
		{
			if ( !state.board.hasMoved(piece))
			{
				// idling is undesired, hence the negative value
				oFactor = -(double)value/10.0;
			}
			if ( oFactor <= -5.0)
				oFactor = -5.0;
		}
		
		
		// return the oFactor divided by 3, this is too make it not too large as it will become an unnecessary pressure
		return oFactor/3.0;
	}
	
	
	/**
	 * Potential factor represents the piece's ability to do advantageous move in the next step, consists of...
	 * moveFactor: the more legal move the piece can make, the higher the value
	 * captureFactor: this method also call the findBestCapture method to evaluate the player's ability to capture a good piece
	 * 
	 * @param state
	 * @param piece
	 * @param pieceName
	 * @return Potential factor
	 */
	private double getPotentialFactor ( State state, Piece piece, String pieceName)
	{
		Board board = state.board;
		
		// only the player who is to move on this turn can benefit from this factor, since it indicates what you can do on your move
		if (!piece.player.equals(state.player))
			return 0.0;
		
		
		double moveFactor = 0.0;

		int file = piece.file;
		int rank = piece.rank;
		
		
		/**
		 * Begin assessment HERE
		 * concept: check every possible square the piece can move to
		 * if the square is empty => the piece can move there => moveFactor increases by 1.0
		 * if the square is not empty, then there's a potential capture the piece can make, 
		 * call method findBestCapture to assess this
		 */
		if ( pieceName.equals("Pawn"))
		{
			file--;
			rank++;
			if ( board.pieceAt(file, rank))
				this.findBestCapture(file, rank, piece, state);
			file = piece.file;
			rank = piece.rank;
			
			file++;
			rank++;
			if ( board.pieceAt(file, rank))
				this.findBestCapture(file, rank, piece, state);
			file = piece.file;
			rank = piece.rank;
		}
		
		
		else if ( pieceName.equals("King"))
		{
			file--;
			rank++;
			if ( board.pieceAt(file, rank))
				this.findBestCapture(file, rank, piece, state);
			file = piece.file;
			rank = piece.rank;
			
			file++;
			rank++;
			if ( board.pieceAt(file, rank))
				this.findBestCapture(file, rank, piece, state);
			file = piece.file;
			rank = piece.rank;
			
			file--;
			rank--;
			if ( board.pieceAt(file, rank))
				this.findBestCapture(file, rank, piece, state);
			file = piece.file;
			rank = piece.rank;
			
			file++;
			rank--;
			if ( board.pieceAt(file, rank))
				this.findBestCapture(file, rank, piece, state);
			file = piece.file;
			rank = piece.rank;
			
			file--;
			if ( board.pieceAt(file, rank))
				this.findBestCapture(file, rank, piece, state);
			file = piece.file;
			
			file++;
			if ( board.pieceAt(file, rank))
				this.findBestCapture(file, rank, piece, state);
			file = piece.file;
			
			rank--;
			if ( board.pieceAt(file, rank))
				this.findBestCapture(file, rank, piece, state);
			rank = piece.rank;
			
			rank++;
			if ( board.pieceAt(file, rank))
				this.findBestCapture(file, rank, piece, state);
			rank = piece.rank;
		}
		
		
		
		else if ( pieceName.equals("Rook"))
		{
			file--; 
			while ( !board.pieceAt(file, rank) && Board.isValid(file, rank))
			{
				file--; 
				moveFactor += 1.0;
			}
			this.findBestCapture(file, rank, piece, state);
			file = piece.file;
			
			file++; 
			while ( !board.pieceAt(file, rank) && Board.isValid(file, rank))
			{
				file++; 
				moveFactor += 1.0;
			}
			this.findBestCapture(file, rank, piece, state);
			file = piece.file;
			
			rank++; 
			while ( !board.pieceAt(file, rank) && Board.isValid(file, rank))
			{
				rank++; 
				moveFactor += 1.0;
			}
			this.findBestCapture(file, rank, piece, state);
			rank = piece.rank;
			
			rank--; 
			while ( !board.pieceAt(file, rank) && Board.isValid(file, rank))
			{
				rank--; 
				moveFactor += 1.0;
			}
			this.findBestCapture(file, rank, piece, state);
			rank = piece.rank;
		}
		
		
		else if ( pieceName.equals("Queen"))
		{
			file--; 
			while ( !board.pieceAt(file, rank) && Board.isValid(file, rank))
			{
				file--; 
				moveFactor += 1.0;
			}
			this.findBestCapture(file, rank, piece, state);
			file = piece.file;
			
			file++; 
			while ( !board.pieceAt(file, rank) && Board.isValid(file, rank))
			{
				file++; 
				moveFactor += 1.0;
			}
			this.findBestCapture(file, rank, piece, state);
			file = piece.file;
			
			rank++; 
			while ( !board.pieceAt(file, rank) && Board.isValid(file, rank))
			{
				rank++; 
				moveFactor += 1.0;
			}
			this.findBestCapture(file, rank, piece, state);
			rank = piece.rank;
			
			rank--; 
			while ( !board.pieceAt(file, rank) && Board.isValid(file, rank))
			{
				rank--; 
				moveFactor += 1.0;
			}
			this.findBestCapture(file, rank, piece, state);
			rank = piece.rank;
			
			rank--; 
			file--;
			while ( !board.pieceAt(file, rank) && Board.isValid(file, rank))
			{
				rank--; 
				file--;
				moveFactor += 1.0;
			}
			this.findBestCapture(file, rank, piece, state);
			rank = piece.rank;
			file = piece.file;
			
			rank--; 
			file++;
			while ( !board.pieceAt(file, rank) && Board.isValid(file, rank))
			{
				rank--; 
				file++;
				moveFactor += 1.0;
			}
			this.findBestCapture(file, rank, piece, state);
			rank = piece.rank;
			file = piece.file;
			
			rank++; 
			file--;
			while ( !board.pieceAt(file, rank) && Board.isValid(file, rank))
			{
				rank++; 
				file--;
				moveFactor += 1.0;
			}
			this.findBestCapture(file, rank, piece, state);
			rank = piece.rank;
			file = piece.file;
			
			rank++; 
			file++;
			while ( !board.pieceAt(file, rank) && Board.isValid(file, rank))
			{
				rank++; 
				file++;
				moveFactor += 1.0;
			}
			this.findBestCapture(file, rank, piece, state);
			rank = piece.rank;
			file = piece.file;
			
			moveFactor /= 2.0;
		}
		
		
		else if ( pieceName.equals("Bishop"))
		{
			rank--; 
			file--;
			while ( !board.pieceAt(file, rank) && Board.isValid(file, rank))
			{
				rank--; 
				file--;
				moveFactor += 1.0;
			}
			this.findBestCapture(file, rank, piece, state);
			rank = piece.rank;
			file = piece.file;
			
			rank--; 
			file++;
			while ( !board.pieceAt(file, rank) && Board.isValid(file, rank))
			{
				rank--; 
				file++;
				moveFactor += 1.0;
			}
			this.findBestCapture(file, rank, piece, state);
			rank = piece.rank;
			file = piece.file;
			
			rank++; 
			file--;
			while ( !board.pieceAt(file, rank) && Board.isValid(file, rank))
			{
				rank++; 
				file--;
				moveFactor += 1.0;
			}
			this.findBestCapture(file, rank, piece, state);
			rank = piece.rank;
			file = piece.file;
			
			rank++; 
			file++;
			while ( !board.pieceAt(file, rank) && Board.isValid(file, rank))
			{
				rank++; 
				file++;
				moveFactor += 1.0;
			}
			this.findBestCapture(file, rank, piece, state);
			rank = piece.rank;
			file = piece.file;
			
		}
		
		
		else if ( pieceName.equals("Knight"))
		{
			rank--;
			rank--;
			file--;
			while ( !board.pieceAt(file, rank) && Board.isValid(file, rank))
			{
				rank--; 
				rank--;
				file--;
				moveFactor += 1.0;
			}
			this.findBestCapture(file, rank, piece, state);
			rank = piece.rank;
			file = piece.file;
			
			rank--;
			rank--;
			file++;
			while ( !board.pieceAt(file, rank) && Board.isValid(file, rank))
			{
				rank--; 
				rank--;
				file++;
				moveFactor += 1.0;
			}
			this.findBestCapture(file, rank, piece, state);
			rank = piece.rank;
			file = piece.file;
			
			rank++;
			rank++;
			file--;
			while ( !board.pieceAt(file, rank) && Board.isValid(file, rank))
			{
				rank++; 
				rank++;
				file--;
				moveFactor += 1.0;
			}
			this.findBestCapture(file, rank, piece, state);
			rank = piece.rank;
			file = piece.file;
			
			rank++;
			rank++;
			file++;
			while ( !board.pieceAt(file, rank) && Board.isValid(file, rank))
			{
				rank++; 
				rank++;
				file++;
				moveFactor += 1.0;
			}
			this.findBestCapture(file, rank, piece, state);
			rank = piece.rank;
			file = piece.file;
			
			rank--;
			file--;
			file--;
			while ( !board.pieceAt(file, rank) && Board.isValid(file, rank))
			{
				rank--; 
				file--;
				file--;
				moveFactor += 1.0;
			}
			this.findBestCapture(file, rank, piece, state);
			rank = piece.rank;
			file = piece.file;
			
			rank++;
			file--;
			file--;
			while ( !board.pieceAt(file, rank) && Board.isValid(file, rank))
			{
				rank++; 
				file--;
				file--;
				moveFactor += 1.0;
			}
			this.findBestCapture(file, rank, piece, state);
			rank = piece.rank;
			file = piece.file;
			
			rank--;
			file++;
			file++;
			while ( !board.pieceAt(file, rank) && Board.isValid(file, rank))
			{
				rank--; 
				file++;
				file++;
				moveFactor += 1.0;
			}
			this.findBestCapture(file, rank, piece, state);
			rank = piece.rank;
			file = piece.file;
			
			rank++;
			file++;
			file++;
			while ( !board.pieceAt(file, rank) && Board.isValid(file, rank))
			{
				rank++; 
				file++;
				file++;
				moveFactor += 1.0;
			}
			this.findBestCapture(file, rank, piece, state);
			rank = piece.rank;
			file = piece.file;
			
		}
		
		// return moveFactor divided by 3, again to avoid it getting too large
		return moveFactor/3.0;
	} // end of getPotentialFactor()
	
	
	

	/**
	 * Find the value of the best piece the player can capture in this turn
	 * 
	 * @param file
	 * @param rank
	 * @param piece
	 * @param state
	 * @return the best value can obtain
	 */
	private void findBestCapture( int file, int rank, Piece piece, State state)
	{
		Piece pieceOnPath = null;
		double captureValue = 0.0;
		
		if ( Board.isValid(file, rank))
		{
			// the piece on this piece's path
			pieceOnPath = state.board.getPieceAt(file, rank);
			
			// chance to capture opponent's piece
			if ( !pieceOnPath.player.equals(piece.player))
				captureValue = this.getPieceValue(pieceOnPath);
		}
		
		// the bestCapture field holds the best possible value
		this.bestCapture = Math.max(this.bestCapture, captureValue); 
	}
	
	
	/**
	 * Evaluate whether castling has occurred for my BOT
	 * return 5.0 if yes - a positive 5.0 indicate desirable state
	 * 0.0 if no
	 */
	// 
	private double getCastlingFactor( State state)
	{
		Piece currentKing = state.board.getKing(mySide);
		Piece pastKing = state.previous.board.getKing(mySide);
		
		if ( Math.abs(currentKing.file - pastKing.file) == 2) // YES
		{
			return 5.0;
		}
		else
			return 0.0;
	}
}
