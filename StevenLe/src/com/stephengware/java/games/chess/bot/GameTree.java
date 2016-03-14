package com.stephengware.java.games.chess.bot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import com.stephengware.java.games.chess.state.State;

/**
 * A game tree is a representation of a state and all of its related data, such as...
 * 
 * state - the state it represents
 * parent - the previous state
 * children - the list of all of its children
 * nextStates - the iterator of all of its next states
 * value - the evaluated value of the state
 * stateName - the string representation of the state
 * 
 * @author Hung L. Le
 */


public class GameTree {

	/** The current state of the game */
	public final State state;
	
	/** The parent node of this tree (i.e. the state before this state) */
	public final GameTree parent;
	
	/** This node's children nodes (i.e. all possible next states) */
	public final ArrayList<GameTree> children = new ArrayList<>();
	
	/** An iterator of the next possible states */
	private Iterator<State> nextStates;
	
	/** The utility value of this state (i.e. how desirable it is for the player) */
	public double value = 0.0f;
	
	/** The string representation of the node */
	public String stateName;
		
	/**
	 * Constructs a new game tree with some initial state as the parent.
	 * 
	 * @param initial the initial state of the game
	 */
	public GameTree(State initial) {
		this(initial, null);
	}
	
	/**
	 * Constructs a new game tree with a current state that resulted from
	 * taking a given move.
	 * 
	 * @param state the state of the game after making that move
	 * @param parent the parent node (i.e. the previous state)
	 */
	protected GameTree(State state, GameTree parent) {
		this.state = state;
		this.parent = parent;
		this.nextStates = state.next().iterator();
		this.stateName = null;
	}
	
	/**
	 * Returns the state of this tree's parent
	 * 
	 * @return the state of this tree's parent
	 */
	public State getState() {
		return this.state;
	}
	
	/**
	 * Returns true if this node has more children nodes which have not yet
	 * been expanded.
	 * 
	 * @return true if there are more children nodes to add, false otherwise
	 */
	public boolean hasNextChild() {
		return nextStates.hasNext();
	}
	
	/**
	 * Constructs and returns the next child node of this node.
	 * 
	 * @return the next child node
	 */
	public GameTree getNextChild() {
		State state = nextStates.next();
		
		GameTree child = new GameTree(state, this );
		if ( this.parent == null) // this is the root node
		{
			children.add(child);
		}
		return child;
	}
	
	
	
	
	/**
	 * <IMPORTANT> perform move reordering for the sake of pruning and get the tree ready for the next iteration
	 * Expected outcome: this.nextState holds a new iterator, now in descending order of values
	 * 
	 * @param previousStatesMap - the HashMap that keeps track of previous states, used to check draw state
	 * @return The Best Node found in this tree's children list
	 */
	public GameTree reorderGameTree ( HashMap<String,Integer> previousStatesMap )
	{

		/**
		 * Copy all values of this Tree's children into a value array
		 * use MergeSort to sort this value array in descending order
		 */
		MyMergeSort mySort = new MyMergeSort();
		
		double[] valueArray = new double[this.children.size()];
		for ( int i = 0; i < this.children.size(); i++)
		{
			valueArray[i] = this.children.get(i).value;
		}
		
		mySort.sort(valueArray);
		
		/**
		 * Add all nodes of this Tree's children to the sortedNodeList
		 * With their values in descending order
		 */
		GameTree bestNode = null;
		
		ArrayList<GameTree> sortedNodeList = new ArrayList<GameTree>(); 
		
		for ( int i = 0; i < valueArray.length; i++)
		{
			for ( int j = 0; j < this.children.size(); j++)
			{
				if ( this.children.get(j).value == valueArray[i] )
				{
					
					/**
					 *  call checkDrawState method to ensure this state is not a draw state
					 *  only proceed with this state if it is NOT a draw state
					 */
					if ( !this.checkDrawState(this.children.get(j), previousStatesMap) )
					{
						sortedNodeList.add(this.children.get(j));
						
						// save the first node in the sortedNodeList, i.e. the highest value state
						if ( sortedNodeList.size() == 1 )
						{
							bestNode = sortedNodeList.get(0);
						}
					}
					
					/**
					 *  if this is the last node in the list but we haven't found any state to proceed with
					 *  then we have no choice but to accept a draw state.
					 *  otherwise, the game would throw an exception as no state is found at all
					 */
					else if ( (j == this.children.size()-1 ) && sortedNodeList.size() == 0)
					{
						sortedNodeList.add(this.children.get(j));

						bestNode = sortedNodeList.get(0);

					}
					
					/**
					 * remove the children from the tree's children list
					 * after this loop, the list should be empty
					 * so that it is in fresh state and ready for a new iteration
					 */
					this.children.remove(j);
					
					break;
				}
			}

		}
		
		// call putCaptureMoveOntop to get a list in the same descending order, but with capture moves on top
		// this is to improve pruning
		ArrayList<State> sortedStateList = this.putCaptureMoveOntop(sortedNodeList);
		
		/**
		 *  get an iterator from sortedStatesList and save it to this.nextStates
		 *  now this tree has everything it needs for a new iteration
		 */
		this.nextStates = sortedStateList.iterator();
		
		// return the bestNode found in this iteration
		return bestNode;
	}
	
	
	
	/**
	 * Check whether or not this state is a draw state
	 * 
	 * @param stateToCheck
	 * @param previousStatesMap
	 * @return true if this is a draw state, otherwise false
	 */
	public boolean checkDrawState( GameTree nodeToCheck, HashMap<String,Integer> previousStatesMap )
	{
		State stateToCheck = nodeToCheck.getState();
		
		// get the state's string representation
		String stateName = stateToCheck.toString().substring(stateToCheck.toString().lastIndexOf(" "));
	
		// if there has been two of the same states occurred in the past, then another one will lead to a draw
		if ( previousStatesMap.containsKey(stateName) && previousStatesMap.get(stateName) == 2)
		{
			return true;
		}
		
		// save the stateName for later use
		nodeToCheck.stateName = stateName;
		
		return false;
	}
	
	
	/**
	 * Do further move reordering by putting capturing move on top of the list
	 * 
	 * @param stateToCheck
	 * @param previousStatesMap
	 * @return true if this is a draw state, otherwise false
	 */
	public ArrayList<State> putCaptureMoveOntop( ArrayList<GameTree> sortedNodeList)
	{
		ArrayList<State> captureFirstList = new ArrayList<State>();
		ArrayList<State> nonCaptureMoves = new ArrayList<State>();
		
		// put all capture moves in captureFirstList list, otherwise in the other
		for ( GameTree node: sortedNodeList)
		{
			if ( node.stateName.contains("x")) //indicates capture moves
			{
				captureFirstList.add(node.getState());
			}
			else
			{
				nonCaptureMoves.add(node.getState());
			}
		}
		
		// add nonCaptureMove list to captureFirstList
		// now captureFirstList contains all the move without messing up the original order, only with capture moves on top
		captureFirstList.addAll(nonCaptureMoves);
		
		return captureFirstList;
	}
	
	
	/**
	 * Mergesort class
	 * only used for the sake of move ordering
	 */
	private class MyMergeSort {
	     
	    private double[] array;
	    private double[] tempMergArr;
	    private int length;
	     
	    public void sort(double[] inputArr) {
	        this.array = inputArr;
	        this.length = inputArr.length;
	        this.tempMergArr = new double[length];
	        doMergeSort(0, length - 1);
	    }
	 
	    private void doMergeSort(int lowerIndex, int higherIndex) {
	         
	        if (lowerIndex < higherIndex) {
	            int middle = lowerIndex + (higherIndex - lowerIndex) / 2;
	            // Below step sorts the left side of the array
	            doMergeSort(lowerIndex, middle);
	            // Below step sorts the right side of the array
	            doMergeSort(middle + 1, higherIndex);
	            // Now merge both sides
	            mergeParts(lowerIndex, middle, higherIndex);
	        }
	    }
	 
	    private void mergeParts(int lowerIndex, int middle, int higherIndex) {
	 
	        for (int i = lowerIndex; i <= higherIndex; i++) {
	            tempMergArr[i] = array[i];
	        }
	        int i = lowerIndex;
	        int j = middle + 1;
	        int k = lowerIndex;
	        while (i <= middle && j <= higherIndex) {
	            if (tempMergArr[i] >= tempMergArr[j]) {
	                array[k] = tempMergArr[i];
	                i++;
	            } else {
	                array[k] = tempMergArr[j];
	                j++;
	            }
	            k++;
	        }
	        while (i <= middle) {
	            array[k] = tempMergArr[i];
	            k++;
	            i++;
	        }
	 
	    }
	}
}