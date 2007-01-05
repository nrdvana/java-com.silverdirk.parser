package com.silverdirk.parser;

import com.silverdirk.datastruct.*;
import java.util.*;

/**
 * <p>All concepts used in the code below and in related classes can be found in
 * a large number of books, and around the Internet.  The one I used in my
 * Automata course is
 * <div style="padding-left:1em">
 * An Introduction to Formal Languages and Automata, 3rd ed.<br/>
 * by Peter Linz<br/>
 * Jones and Bartlett Publishers, 2001<br/>
 * ISBN 0-7637-1442-4<br/>
 * </div>
 *
 * <h3>Terminology:</h3>
 * <dl>
 * <dt>NFA<dd>Nondeterministic Finite Acceptor:  A state machine where you can
 * 	transition from state to state sometimes consuming a character of
 * 	input, and sometimes not.  Transitions (edges) that can be performed
 * 	without consuming a character are "lambda edges", and the existance
 * 	of these edges mean that you never really know what state you are in.
 * 	A fun physics-inspired way of thinking about this is that you are
 * 	"in multiple states at once".  Also, there can be more than one
 * 	outgoing edge that uses the same character, so if that character of
 * 	input is encountered at the node, you have to follow "both edges".
 * <dt>DFA<dd>Deterministic Finite Acceptor:  A state machine where at any state
 * 	you can absolutely determine which state to go to next based on the
 * 	next character of input.
 * <dt>Edge<dd> Graph terminology.  State machines are a graph of states.  The lines
 * 	between states are 'edges' of the graph.
 * <dt>Transition<dd>In this context, synonymous with 'edge'.  A transition from
 * 	one state to the next is represented by an edge of the graph.
 * <dt>Lambda<dd> The greek character used to mean "no character" in state-machine
 * 	terminology.  Call it NULL if you want, or something.  Lambda edges
 * 	only appear in NFAs.  DFA transitions always consume a real character.
 * <dt>Final State<dd>Automata terminology.  A final state is one where you
 * 	are allowed to quit.  For example, when looking for the string "foo",
 * 	state 0 transitions to 1 on 'f', state 1 to state 2 on 'o', state 2 to
 * 	state 3 on 'o', and state 3 is a final state, meaning you have found it
 * 	and can stop now.
 * <dt>Regex<dd>Regular expression. A regex is a text notation for specifying
 *  NFAs.  Operating on a NFA is somewhat inefficient, so people compile them
 *  to DFAs first.
 * <dt>Compile<dd>Convert a NFA to a DFA using a nifty algorithm that makes DFA
 * 	states for every unique reachable combination of simultaneous states
 * 	in the NFA.  If you are in a state of the DFA, then you know a set of
 * 	NFA states that you might be in.
 * </dl>
 *
 * <p>In conversion from NFA to DFA, each created DFA state contains a list of
 * the NFA states that it representa.  This would be a simple conversion from
 * NFA to DFA, if not for the Groups.  When traversal of the NFA enters a group
 * marker, any states subsequent to that need to have their group actions
 * recorded in a new "GroupSuperposition".  In the current design, there is one
 * superposition in the DFA node per included NFA node.   Only NFA nodes with
 * non-lambda exits are included in this set- other nodes are traversed and
 * discarded after their exits are explored.
 *
 * <p>Each node in DFA node gets sets of "InProgress", "MaybeFound".
 * Reached nodes inherit the InProgress and MaybeFound sets of the referring node.
 * If a 'groupstart' node is traversed, add that group to the InProgress set, and add a 'markGroup x' to the DFA node
 * If a 'groupEnd' node is traversed, remove that group from InProgress and add it to MaybeFound, and add a 'endGroup x' to the DFA node
 * Starting from a set of NFA nodes, absorb all states reachable by lambda.
 *  For each GrouEnd
 *
 * From the new set of DFA nodes, find the set of outward paths.
 * For each path, find the set of nodes reachable from following that path.
 */
public class RegexCompiler {
	NFAGraph source;
	HashMap	dfaNodeMap= new HashMap();
	List dfaNodes= new ArrayList();

	RegexCompiler(NFAGraph source) {
		this.source= source;
	}

	class DFANode {
		int idx;
		BitFieldSet nfaNodes;
		Map groupOpsToNfaNode;
		List transitions= new ArrayList();

		public DFANode(BitFieldSet nfaNodes, Map groupOpsToNfaNode) {
			this.nfaNodes= nfaNodes;
			this.groupOpsToNfaNode= groupOpsToNfaNode;
			dfaNodeMap.put(nfaNodes, this);
			idx= dfaNodes.size();
			dfaNodes.add(this);
		}
	}

	DFAGraph compile() {
		LinkedList workList= new LinkedList();
		HashMap groupPaths= new HashMap();
		workList.add(new DFANode(findReachableNfaNodes(0, groupPaths), groupPaths));
		while (workList.size() > 0) {
			DFANode node= (DFANode) workList.removeFirst();
			Iterator transitionNodes= node.nfaNodes.iterator();
			while (transitionNodes.hasNext()) {
				Integer nodeId= (Integer) transitionNodes.next();

			}
		}
		return null;
	}

	BitFieldSet findReachableNfaNodes(int fromNode, Map groupPaths) {
		BitFieldSet result= new BitFieldSet(source.nodeCount());
		BitFieldSet seen= new BitFieldSet(source.nodeCount());
		LinkedList workList= new LinkedList();
		groupPaths.clear();
		includeNode(fromNode, null, workList, seen, groupPaths);
		while (workList.size() > 0) {
			Integer node= (Integer) workList.removeFirst();
			int nodeId= node.intValue();
			int[] prevPath= (int[]) groupPaths.get(node);
			if (prevPath == null)
				prevPath= new int[0];
			for (int i= source.getExitCount(nodeId)-1; i>= 0; i--)
				if (source.getTransitionChar(nodeId, i) != null)
					result.add(node);
				else {
					int peer= source.getExitPeer(nodeId, i);
					if (!seen.containsIdx(peer))
						includeNode(peer, prevPath, workList, seen, groupPaths);
				}
		}
		// clean up group paths to only include relevant nodes
		for (Iterator i= groupPaths.keySet().iterator(); i.hasNext();)
			if (!result.contains(i.next()))
				i.remove();
		return result;
	}
	private void includeNode(int nodeId, int[] groupPath, LinkedList workList, BitFieldSet seen, Map groupPaths) {
		Integer node= new Integer(nodeId);
		workList.add(node);
		seen.addIdx(nodeId);
		int groupCode= source.getGroupCode(nodeId);
		if (groupCode != 0) {
			int[] newPath;
			if (groupPath == null)
				newPath= new int[] { groupCode };
			else {
				newPath= new int[groupPath.length+1];
				System.arraycopy(groupPath, 0, newPath, 0, groupPath.length);
				newPath[newPath.length-1]= groupCode;
			}
			groupPaths.put(node, newPath);
		}
	}

	DFATransition buildDfaTransition(DFANode from, SetOfChar transChars, DFANode to) {
		// for each node in 'from', if it can reach anything via 'transChars'
		// then build paths to each of the nodes it can reach.  If a node has
		// already been reached, then skip the new path, giving "credit" to the
		// first path that reaches it (thus matching groups on the left of the
		// regex before the groups on the right).  This assumes iterating
		// through nodes in numerical order.
		throw new UnsupportedOperationException();
	}
}

abstract class NFAGraph {
	int groupCount;

	abstract int nodeCount();
	int getGroupCount() {
		return groupCount;
	}
	abstract int getExitCount(int nodeId);
	abstract int getExitPeer(int nodeId, int exitId);
	abstract SetOfChar getTransitionChar(int nodeId, int exitId);
	abstract int getGroupCode(int nodeId);

	protected static void checkNodeId(int nodeId, int nodeCount) {
		if (nodeId >= nodeCount)
			throw new RuntimeException("Node "+nodeId+" is not in this subgraph");
	}

	protected static void checkExitId(int exitId, int exitCount) {
		if (exitId >= exitCount)
			throw new RuntimeException("Specified node has only "+exitCount+" exits");
	}

	public static final SetOfChar LAMBDA= null;
}

class DFAGraph {
	DFATransition[][] edges;
	BitFieldSet finalStates;

	int nodeCount() {
		return edges.length;
	}

	int getExitCount(int nodeId) {
		return edges[nodeId].length;
	}

	int getNodeExitPeer(int nodeId, int exitId) {
		return edges[nodeId][exitId].targetNodeId;
	}
	SetOfChar getNodeExitChar(int nodeId, int exitId) {
		return edges[nodeId][exitId].charSet;
	}
	boolean isFinalState(int nodeId) {
//		return finalStates.contains(nodeId);
		throw new UnsupportedOperationException();
	}
}

class GroupMarkers {
	int[] ranges;
	int[] startMarkers;

	GroupMarkers(int groupCount) {
		ranges= new int[groupCount<<1];
		java.util.Arrays.fill(ranges, 0);
		startMarkers= new int[groupCount];
		java.util.Arrays.fill(startMarkers, 0);
	}

	GroupMarkers(GroupMarkers prev) {
		ranges= prev.ranges;
		startMarkers= new int[prev.startMarkers.length];
		System.arraycopy(prev.startMarkers, 0, startMarkers, 0, prev.startMarkers.length);
	}

	void startGroup(int groupNum, int pos) {
		startMarkers[groupNum]= pos;
	}

	void endGroup(int groupNum, int pos) {
		int[] newRanges= new int[ranges.length];
		System.arraycopy(ranges, 0, newRanges, 0, ranges.length);
		newRanges[groupNum<<1]= startMarkers[groupNum];
		newRanges[(groupNum<<1)+1]= pos;
		ranges= newRanges;
	}
}

class GroupOps {
	int prevStateVar;
	int[] ops;

	static final Object makeGroupStart(int groupNum) {
		return new Integer(groupNum+1);
	}

	static final Object makeGroupEnd(int groupNum) {
		return new Integer(~groupNum);
	}

	public GroupOps(int prevStateVarIdx, List opList) {
		prevStateVar= prevStateVarIdx;
		ops= new int[opList.size()];
		for (int i=0; i<ops.length; i++)
			ops[i]= ((Integer)opList.get(i)).intValue();
	}

	void apply(GroupMarkers state, int charPos) {
		for (int i=0; i<ops.length; i++)
			if (ops[i] > 0)
				state.startGroup(ops[i]-1, charPos);
			else
				state.endGroup(~ops[i], charPos);
	}
}

class DFATransition {
	SetOfChar charSet;
	int targetNodeId;
	GroupOps[] groupOps;
}
