package mcts;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Random;

import enumerate.Action;
import parameter.FixParameter;
import simulator.Simulator;
import struct.FrameData;

public class MCTS {

	/** 乱数を利用するときに使う */
	private Random rnd;

	private Node rootNode;

	/** ノードの深さ */
	private int depth;
	
	/** ノードの最大の深さ */
//	public int mDepth;

	/** ノードが探索された回数 */
	private int totalGames;

	/** シミュレーションするときに利用する */
	private Simulator simulator;

	/** シミュレーションする前の自分のHP */
	private int myOriginalHp;

	/** シミュレーションする前の相手のHP */
	private int oppOriginalHp;

	private boolean playerNumber;

	private Deque<Action> mAction;
	private Deque<Action> oppAction;

	/** 選択できる自分の全Action */
	private LinkedList<Action> availableMyActions;

	/** 選択できる相手の全Action */
	private LinkedList<Action> availableOppActions;

	/** フレームデータ(キャラ情報等) */
	private FrameData frameData;

	private double alpha = 0.5;

	//kusano program start
//	private static final String[] playerMotionName = { "FORWARD_WALK", "DASH", "BACK_STEP", "JUMP", "FOR_JUMP",
//			"BACK_JUMP", "STAND_GUARD", "CROUCH_GUARD", "STAND_A", "STAND_B", "THROW_A", "THROW_B", "CROUCH_A",
//			"CROUCH_B", "STAND_FA", "STAND_FB", "CROUCH_FA", "CROUCH_FB", "STAND_F_D_DFA", "STAND_D_DB_BA",
//			"STAND_D_DB_BB", "STAND_D_DF_FA" };
//
//	private static final String[] aiMotionName = { "FORWARD_WALK", "DASH", "BACK_STEP", "JUMP", "FOR_JUMP", "BACK_JUMP",
//			"STAND_GUARD", "CROUCH_GUARD", "STAND_A", "STAND_B", "THROW_A", "THROW_B", "CROUCH_A", "CROUCH_B",
//			"STAND_FA", "STAND_FB", "CROUCH_FA", "CROUCH_FB", "STAND_F_D_DFA", "STAND_D_DB_BA", "STAND_D_DB_BB",
//			"STAND_D_DF_FA" };


	public ArrayList<String[]> castr;

	public String line;

	public String[] sline;
	//kusano program end
	
	//TODO PDA AI parameter start
	public File cmdFile;//CMD file
	
	public File tableFile;//body usage of motions' table
	
	public String playerTakingAction;
	
	public int actionNumber;
	
	public float[] bodySegmentTotalUsage = {(float) 0.0,(float) 0.0,(float) 0.0,(float) 0.0};//movement of body in total of each game ArmR, ArmL, LegR, LrgL
	
	public float[] bodySegmentTotalUsageGap = {(float) 0.0,(float) 0.0};//difference between movement of body in total of each game Arm, Leg, Negative if R > L
	
	public float[] bodySegmentUsage = {(float) 0.0,(float) 0.0,(float) 0.0,(float) 0.0};//one time movement of body of each game ArmR, ArmL, LegR, LrgL
	
	public float[] bodySegmentUsageGap = {(float) 0.0,(float) 0.0};//difference between one time movement of body in total of each game Arm, Leg, Negative if R > L
	
	public float[] formerBodySegmentTotalUsage = {(float) 0.0,(float) 0.0,(float) 0.0,(float) 0.0};
	
	public float[] formerBodySegmentTotalUsageGap = {(float) 0.0,(float) 0.0};
	
	public ArrayList<String[]> str;
	
	public ArrayList<float[]> motionMovementList;
	
	public float[] fline = {(float)0.0,(float)0.0,(float)0.0,(float)0.0};// movement data
	
	public String[] movementNameList = new String[25];// name for movement data
	
	public float probability;//probability calculated by table
	
	public boolean decision;//decision calculated by probability
	
	public float mappingParameter = (float) 0.0;
	
	//PDA AI parameter end
	
	
	
	
	public MCTS(Node node, FrameData fd, Simulator sim, int myHp, int oppHp, LinkedList<Action> myActions,
			LinkedList<Action> oppActions, boolean p) {

		rootNode = node; // ルートノードの情報を格納
		frameData = fd;
		simulator = sim;
		playerNumber = p;
		myOriginalHp = myHp;
		oppOriginalHp = oppHp;
		availableMyActions = myActions;
		availableOppActions = oppActions;

		mAction = new LinkedList<Action>();
		oppAction = new LinkedList<Action>();
		
//		mDepth = 0;
		
		alpha = (Math.tanh((double) (myOriginalHp - oppOriginalHp) / FixParameter.TANH_SCALE ) + 1) / 2;
		
		//TODO PDA
		//read
		//readMotionDataFromFile();

	}

	/**
	 * MCTSを行う
	 *
	 * @return 最終的なノードの探索回数が多いAction
	 */
	public Action runMcts() {
				
		long start = System.nanoTime();
		for (; System.nanoTime() - start <= FixParameter.UCT_TIME;) {
			uct(this.rootNode);
			// counting++;
		}
		// System.out.println(counting);

//		return getBestVisitAction(this.rootNode);
		return getBestScoreAction(this.rootNode);
	}

	private int getDistanceX(FrameData fd) {
		return fd.getCharacter(true).getLeft() < fd.getCharacter(false).getLeft() ? Math.max(fd.getCharacter(false).getLeft() - fd.getCharacter(true).getRight(), 0)
				: Math.max(fd.getCharacter(true).getLeft() - fd.getCharacter(false).getRight(), 0);
	}

	/**
	 * プレイアウト(シミュレーション)を行う
	 *
	 * @return プレイアウト結果の評価値
	 */
	public double playout(Node selectedNode) {

		mAction.clear();
		oppAction.clear();

		int distance = getDistanceX(frameData);

		LinkedList<Action> selectedMyActions = selectedNode.selectedActionFromRoot();
		rnd = new SecureRandom();

		for (int i = 0; i < selectedMyActions.size(); i++) {
			mAction.add(selectedMyActions.get(i));
		}

		// 予測を使ったならその結果をシミュレーションに渡す
		if (FixParameter.PREDICT_FLAG) {
			int predictSize = 0;
			for (int i = 0; i < 5; i++) {
				oppAction.add(Prediction.getInstance().predict(distance, availableOppActions));
				predictSize++;
			}

			for (int i = 0; i < 5 - predictSize; i++) {
				oppAction.add(availableOppActions.get(rnd.nextInt(availableOppActions.size())));
			}
		} else {
			for (int i = 0; i < 5; i++) {
				oppAction.add(availableOppActions.get(rnd.nextInt(availableOppActions.size())));
			}
		}
		// シミュレーションを実行
		FrameData nFrameData = simulator.simulate(frameData, playerNumber, mAction, oppAction, FixParameter.SIMULATION_TIME);

		mAction.clear();
		oppAction.clear();
		for (int i = 0; i < 5; i++) {
			mAction.add(availableMyActions.get(rnd.nextInt(availableMyActions.size())));
		}
		if (FixParameter.PREDICT_FLAG) {
			int predictSize = 0;
			for (int i = 0; i < 5; i++) {
				oppAction.add(Prediction.getInstance().predict(distance, availableOppActions));
				predictSize++;
			}

			for (int i = 0; i < 5 - predictSize; i++) {
				oppAction.add(availableOppActions.get(rnd.nextInt(availableOppActions.size())));
			}
		} else {
			for (int i = 0; i < 5; i++) {
				oppAction.add(availableOppActions.get(rnd.nextInt(availableOppActions.size())));
			}
		}
		
		// シミュレーションを実行
		nFrameData = simulator.simulate(nFrameData, playerNumber, mAction, oppAction, FixParameter.SIMULATION_TIME);
		return getScore(nFrameData);
	}

	/**
	 * UCTを行う <br>
	 *
	 * @return 評価値
	 */
	public double uct(Node parent) {

		Node selectedNode = null;
		double bestUcb;
		rnd = new SecureRandom();

		bestUcb = -99999;

		double scoreMax = -999;
		double scoreMin = 999;

		parent.games++;
		
		if (FixParameter.NORMALIZED_FLAG) {
			for (Node child : parent.children) {
				double current = child.score / child.games;
				if (scoreMax < current) {
					scoreMax = current;
				}
				if (scoreMin > current) {
					scoreMin = current;
				}
			}

			if (scoreMax - scoreMin == 0) {
				scoreMax = 10;
				scoreMin = -10;
			}
		}

		for (Node child : parent.children) {
			if (child.games == 0) {
				child.ucb = 9999 + rnd.nextInt(50);
			} else {
				if (FixParameter.NORMALIZED_FLAG) {
					child.ucb = getUcb(normalizeScore(child.score / child.games, scoreMax, scoreMin), totalGames,
							child.games);
				} else {
					child.ucb = getUcb(child.score / child.games, totalGames, child.games);
				}
			}

			if (bestUcb < child.ucb) {
				selectedNode = child;
				bestUcb = child.ucb;
			}
		}

		double score = 0;
		if (selectedNode.games == 0) {
//			checkDepth(selectedNode);
			score = playout(selectedNode);
		} else {
			if (selectedNode.children == null) {
				if (selectedNode.depth < FixParameter.UCT_TREE_DEPTH) {
					if (FixParameter.UCT_CREATE_NODE_THRESHOULD <= selectedNode.games) {
						createNode(selectedNode);
						selectedNode.isCreateNode = true;
						score = uct(selectedNode);
					} else {
//						checkDepth(selectedNode);
						score = playout(selectedNode);
					}
				} else {
//					checkDepth(selectedNode);
					score = playout(selectedNode);
				}
			} else {
				if (selectedNode.depth < FixParameter.UCT_TREE_DEPTH) {
					score = uct(selectedNode);
				} else {
//					checkDepth(selectedNode);
					playout(selectedNode);
				}
			}
		}

		selectedNode.games++;
		selectedNode.score += score;

		if (depth == 0) {
			totalGames++;
		}

		return score;
	}
	
//	private void checkDepth(Node selectedNode){
//		if (mDepth < selectedNode.depth){
//			mDepth = selectedNode.depth;
//		}
//	}
	/**
	 * ノードを生成する
	 */
	public void createNode(Node parent) {
		parent.children = new Node[availableMyActions.size()];

		for (int i = 0; i < parent.children.length; i++) {
			parent.children[i] = new Node(parent, availableMyActions.get(i));
		}
	}

	/**
	 * 最多訪問回数のノードのActionを返す
	 *
	 * @return 最多訪問回数のノードのAction
	 */
	public Action getBestVisitAction(Node rootNode) {

		int selected = -1;
		double bestGames = -9999;

		for (int i = 0; i < rootNode.children.length; i++) {

			if (FixParameter.DEBUG_MODE) {
				System.out.println("評価値:" + rootNode.children[i].score / rootNode.children[i].games + ",試行回数:"
						+ rootNode.children[i].games + ",ucb:" + rootNode.children[i].ucb + ",Action:"
						+ rootNode.children[i].getAction());
			}

			if (bestGames < rootNode.children[i].games) {
				bestGames = rootNode.children[i].games;
				selected = i;
			}
		}

		if (FixParameter.DEBUG_MODE) {
			System.out.println(rootNode.children[selected].getAction() + ",全試行回数:" + totalGames);
			System.out.println("");
		}

		return rootNode.children[selected].getAction();// availableMyActions.get(selected);
	}

	/**
	 * 最多スコアのノードのActionを返す
	 *
	 * @return 最多スコアのノードのAction
	 */
	public Action getBestScoreAction(Node rootNode) {

		int selected = -1;
		double bestScore = -9999;

		for (int i = 0; i < rootNode.children.length; i++) {

//			System.out.println("評価値:" + rootNode.children[i].score / rootNode.children[i].games + ",試行回数:"
//					+ rootNode.children[i].games + ",ucb:" + rootNode.children[i].ucb + ",Action:"
//					+ rootNode.children[i].getAction());

			double meanScore = rootNode.children[i].score / rootNode.children[i].games;
			if (bestScore < meanScore) {
				bestScore = meanScore;
				selected = i;
			}
		}

//		System.out.println(rootNode.children[selected].getAction() + ",全試行回数:" + totalGames);
//		System.out.println("");

		return rootNode.children[selected].getAction();
	}

	/**
	 * 評価値を返す
	 *
	 * @param fd
	 *            フレームデータ(これにhpとかの情報が入っている)
	 * @return 評価値
	 */
	public double getScore(FrameData fd) {
		double value = 0.0;

		if (FixParameter.MIX_FLAG) {
			value = (1 - alpha) * evalStrength(fd) + alpha * evalTanh(fd);
		} else if (FixParameter.STRONG_FLAG) {
			value = evalDifferenceHP(fd);
		} else {
			value = evalTanh(fd);
		}
		
		//runPdaForHealth();//do PDA
		//System.out.println("decision =" + decision);
//TODO another solution
//		if (decision = true) {
			return value;
//		}	else {
//			return -(1.0) * value;
//		}
	}

	/**
	 * 自分と相手のHPの変化量の差を評価値の基準として返す
	 *
	 * @param フレームデータ
	 *
	 * @return 自分と相手のHPの変化量の差
	 */
	private int evalDifferenceHP(FrameData frameData) {
		if (playerNumber) {
			return (frameData.getCharacter(true).getHp() - myOriginalHp) - (frameData.getCharacter(false).getHp() - oppOriginalHp);
		} else {
			return (frameData.getCharacter(false).getHp() - myOriginalHp) - (frameData.getCharacter(true).getHp() - oppOriginalHp);
		}
	}

	private double evalTanh(FrameData frameData) {
		double score = Math
				.abs(frameData.getCharacter(playerNumber).getHp() - frameData.getCharacter(!playerNumber).getHp());

		return (1 - Math.tanh(score / FixParameter.TANH_SCALE));
	}

	private double evalStrength(FrameData frameData) {
		return Math.tanh(
				(double) (oppOriginalHp - frameData.getCharacter(!playerNumber).getHp()) / FixParameter.TANH_SCALE);
	}

	/**
	 * 評価値と全プレイアウト試行回数とそのActionのプレイアウト試行回数からUCB1値を返す
	 *
	 * @param score
	 *            評価値
	 * @param n
	 *            全プレイアウト試行回数
	 * @param ni
	 *            そのActionのプレイアウト試行回数
	 * @return UCB1値
	 */

	public double getUcb(double score, int n, int ni) {
		return score + FixParameter.UCB_C * Math.sqrt((2 * Math.log(n)) / ni);
	}

	public void printNode(Node node) {
		System.out.println("全試行回数:" + node.games);
		for (int i = 0; i < node.children.length; i++) {
			System.out.println(i + ",回数:" + node.children[i].games + ",深さ:" + node.children[i].depth + ",score:"
					+ node.children[i].score / node.children[i].games + ",ucb:" + node.children[i].ucb);
		}
		System.out.println("");
		for (int i = 0; i < node.children.length; i++) {
			if (node.children[i].isCreateNode) {
				printNode(node.children[i]);
			}
		}
	}

	/**
	 * スコアを正規化する
	 *
	 * @return 正規化されたスコア
	 */
	public double normalizeScore(double score, double scoreMax, double scoreMin) {
		double tmpScore = 0;

		tmpScore = (score - scoreMin) / (double) (scoreMax - scoreMin);
		if (tmpScore > 1) {
			tmpScore = 1;
		} else if (tmpScore < 0) {
			tmpScore = 0;
		}

		return tmpScore;
	}

	//TODO PDA AI function start

	public void runPdaForHealth() {
			
		//PDA addition start
		playerTakingAction = readCMD(cmdFile);
		System.out.println("playerTakingAction = " + playerTakingAction);
		readMotionTable();
		probability = calculateProbability();
		decision = decideOpponentAttitude(probability);
		//PDA addition end
	}
	
	public void readMotionDataFromFile() {
		//read motion file
			try {
				// 各モーションの運動量のファイル
				FileReader fr = new FileReader("data/aiData/Motion.csv");
				BufferedReader br = new BufferedReader(fr);

				this.str = new ArrayList<String[]>();
				this.motionMovementList = new ArrayList<float[]>();
				line = br.readLine();
				System.out.println("succeed read file");
				// 1行ごとにString[]化、リストstrに追加
				while (true) {

					line = br.readLine();

					if (line == null)
						break;				
					sline = line.split(",");
					str.add(sline.clone());
									
				}

				
				  // プレイヤーのカウンターアクション確率 bbr = new
				//FileReader ffr = new FileReader("data/aiData/ActionHistory.csv");
				//BufferedReader bbr = new BufferedReader(ffr);
				 
				// this.castr = new ArrayList<String[]>(); line = bbr.readLine();
				 
				// while (true) { line = bbr.readLine(); if (line == null) break;
				// sline = line.split(","); castr.add(sline.clone()); }
				
				
				//debug
//				for ( int i = 0; i < str.size() ;i++) {
//					String[] s = str.get(i);
//					
//					System.out.println("raw =" + s[0] + " " + s[3] + " " + s[4] + " " + s[9] + " " + s[10]);
//				}
				
				for(int i = 0; i < str.size(); i++) {
					String[] s = this.str.get(i);
					//System.out.println("raw =" + s[0] + " " + s[1] + " " + s[2] + " " + s[3] + " " + s[4]);
					//read motion data from string to float, 0 name 3 legR 4 legL 9 WristR 10 WristL
					this.movementNameList[i] = s[0];
					this.fline[0] = Float.parseFloat(s[3]);
					this.fline[1] = Float.parseFloat(s[4]);
					this.fline[2] = Float.parseFloat(s[9]);
					this.fline[3] = Float.parseFloat(s[10]);
					//System.out.println("fline =" + this.fline[0] + " " + this.fline[1] + " " + this.fline[2] + " " + this.fline[3]);
					motionMovementList.add(fline.clone());
				}
				
				for ( int i = 0; i < motionMovementList.size() ;i++) {
					float[] f = motionMovementList.get(i);
					
					//System.out.println("raw =" + f[0] + " " + f[1] + " " + f[2] + " " + f[3]);
				}
				
				br.close();
			} catch (IOException ex) {
				// 例外発生時処理
				ex.printStackTrace();
			}
			

		//read motion file end
	}
	

	
	//read real-time action from uki
	public String readCMD(File cmdFile){
		try {	
			BufferedReader reader = new BufferedReader(new FileReader("C:\\FTG\\CMD1.txt"));
			int a = 0;
			if ((a = reader.read())!= -1) {
				String output = reader.readLine();
				return output;
			}
			else {
				return "DASH";
			} 

			
		} catch(Exception e) {
			e.getStackTrace();
			return null;
		}
	}
	
	//read one player motion data from converted table of motion.csv
	//also add it into bodySegmentUsage for total count and calculate bodySegmentUsageGap
	public void readMotionTable(){
		
		int counter = 0;
		//float prob = 0;
		//String[] actionNumberString = {"0","0","0","0"};
		//float[] bodySegmentUsage = {(float) 0.0,(float) 0.0,(float) 0.0,(float) 0.0};
		

		for(;counter < this.movementNameList.length; ++counter) {
			//TODO debug
			//System.out.println("p= " + playerTakingAction);
			//System.out.println("m= " +movementNameList[counter].substring(1));
			if (this.playerTakingAction.contains(this.movementNameList[counter].substring(1))) {
				System.out.println("get action " + counter);				
				this.actionNumber = counter;
			} else {
				//System.out.println("no");						
			}
		}
		//check if successfully get actionNumber
		if (this.actionNumber != -1) {
			float[] f = motionMovementList.get(this.actionNumber);
			System.out.println(f[0] + " " + f[1] + " " + f[2] + " " + f[3] + " ");
			for(counter = 0;counter < this.bodySegmentUsage.length;++counter) {
				this.bodySegmentUsage[counter] = f[counter];
				this.bodySegmentTotalUsage[counter] += this.bodySegmentUsage[counter]; 
//				System.out.println("BodySegmentUsage " + counter +  " " +  this.bodySegmentUsage[counter]);		
//				System.out.println("BodySegmentTotalUsage " + counter + " " +  this.bodySegmentTotalUsage[counter]);
	
			}

			for(counter = 0;counter < this.bodySegmentTotalUsageGap.length;++counter) {
				this.bodySegmentTotalUsageGap[counter] = this.bodySegmentTotalUsage[counter * 2 + 1] - this.bodySegmentTotalUsage[counter * 2];	
				//System.out.println("bodySegmentTotalUsageGap " + counter +  this.bodySegmentTotalUsageGap[counter]);					
			}
		

			//TODO debug
	//		for(counter = 0; counter < 4; ++counter) {
	//			System.out.println("Usage" + counter + bodySegmentUsage[counter]);			
	//		}
		}
	}

	public float calculateProbability() {
		
		
		int counter;
		float prob = (float) 33.33;
		float controlParameter = (float) 5.0;
		
		//difference between movement of body in total of each game Arm, Leg, Negative if R > L
		//healthy action should be have different sign between formerBodySegmentUsageGap and MotionDataGap
		//the probability is calculated by formerBodySegmentTotalUsageGap/parameter
		for(counter = 0;counter < this.formerBodySegmentTotalUsage.length;++counter) {
			this.formerBodySegmentTotalUsage[counter] = this.bodySegmentTotalUsage[counter] - this.bodySegmentUsage[counter];
//			System.out.println("formerBodySegmentTotalUsage " + counter +  this.formerBodySegmentTotalUsage[counter]);
//			System.out.println("BodySegmentTotalUsage " + counter +  this.bodySegmentTotalUsage[counter]);
//			System.out.println("BodySegmentUsage " + counter +  this.bodySegmentUsage[counter]);
		}
		
		for(counter = 0;counter < this.formerBodySegmentTotalUsageGap.length;++counter) {
			this.formerBodySegmentTotalUsageGap[counter] = this.formerBodySegmentTotalUsage[counter * 2 + 1] - this.formerBodySegmentTotalUsage[counter * 2];				
		}
		
		for(counter = 0;counter < this.formerBodySegmentTotalUsageGap.length;++counter) {
			//TODO DEBUG
			//System.out.println("FormerTotalUsageGap = " + this.formerBodySegmentTotalUsageGap[counter]);
			//System.out.println("TotalUsageGap = " + counter + " " + this.bodySegmentTotalUsageGap[counter]);
			//System.out.println("bodySegmentUsageGap = " + counter + " " + this.bodySegmentUsageGap[counter]);
		}
		
		for(counter = 0;counter < this.bodySegmentUsageGap.length;++counter) {
			this.bodySegmentUsageGap[counter] = this.bodySegmentUsage[counter * 2 + 1] - this.bodySegmentUsage[counter * 2];				
		}
	
//		if (Math.abs(this.bodySegmentTotalUsageGap[0]) + Math.abs(this.bodySegmentTotalUsageGap[1]) > this.mappingParameter) {
//			this.mappingParameter = Math.abs(this.bodySegmentTotalUsageGap[0]) + Math.abs(this.bodySegmentTotalUsageGap[1]);
//			System.out.println("mappingParameter" + mappingParameter);
//		}
		//TODO	fix formula		
		//health
		if ((Math.abs(this.bodySegmentTotalUsageGap[0]) + Math.abs(this.bodySegmentTotalUsageGap[1]) - Math.abs(this.formerBodySegmentTotalUsageGap[0]) - Math.abs(this.formerBodySegmentTotalUsageGap[1])) < (float)0.0) 
		{
			System.out.println("action is healthy");
			//prob = (float) 0.9 - (float)0.9 *(Math.abs(this.bodySegmentTotalUsageGap[0]) + Math.abs(this.bodySegmentTotalUsageGap[1])) / this.mappingParameter;
			//prob = (float) 0.9 - (float)0.9 *(Math.abs(this.bodySegmentTotalUsageGap[0]) + Math.abs(this.bodySegmentTotalUsageGap[1])) / this.mappingParameter;
			prob = (float)0.9;
		} 
		//unhealth
		else if ((Math.abs(this.bodySegmentTotalUsageGap[0]) + Math.abs(this.bodySegmentTotalUsageGap[1]) - Math.abs(this.formerBodySegmentTotalUsageGap[0]) - Math.abs(this.formerBodySegmentTotalUsageGap[1])) > (float)0.0)
		{		
			System.out.println("action is unhealthy");
			//prob = (float) 0.9 -  (float)0.9 * (Math.abs(this.bodySegmentTotalUsageGap[0]) + Math.abs(this.bodySegmentTotalUsageGap[1])) / this.mappingParameter;
			prob = (float)0.1;
		}
		else 
		{
			prob = (float)0.8;
			System.out.println("Equal");
		}
//TODO		
//		// test dynamic formula of each part
//		System.out.println("total prob= " + (Math.abs(this.bodySegmentUsageGap[0]) + Math.abs(this.bodySegmentUsageGap[1]))/(Math.abs(this.bodySegmentTotalUsageGap[0]) + Math.abs(this.bodySegmentTotalUsageGap[1])));
//		if (this.bodySegmentUsageGap[0] * this.bodySegmentTotalUsageGap[0] < 0 && this.bodySegmentUsageGap[1] * this.bodySegmentTotalUsageGap[1] < 0){
//			//both health	
//			prob = (float)0.9;
//		} else if (this.bodySegmentUsageGap[0] * this.bodySegmentTotalUsageGap[0] > 0 && this.bodySegmentUsageGap[1] * this.bodySegmentTotalUsageGap[1] < 0)
//		{
//			//leg unhealth wrist health
//			prob = (float)0.9 - (float)0.8 * (Math.abs(this.bodySegmentUsageGap[0]))/(Math.abs(this.bodySegmentTotalUsageGap[0]));
//			System.out.println("leg unhealth, leg prob = " + (Math.abs(this.bodySegmentUsageGap[0])/(Math.abs(this.bodySegmentTotalUsageGap[0]))));
//			
//		} else if (bodySegmentUsageGap[0] * bodySegmentTotalUsageGap[0] < 0 && bodySegmentUsageGap[1] * bodySegmentTotalUsageGap[1] > 0) 
//		{
//			//leg health wrist unhealth
//			prob = (float)0.9 - (float)0.8 * (Math.abs(this.bodySegmentUsageGap[1]))/(Math.abs(this.bodySegmentTotalUsageGap[1]));
//			System.out.println("wrist unhealth, wrist prob = " + (Math.abs(this.bodySegmentUsageGap[1])/(Math.abs(this.bodySegmentTotalUsageGap[1]))));
//		} else if (bodySegmentUsageGap[0] * bodySegmentTotalUsageGap[0] > 0 && bodySegmentUsageGap[1] * bodySegmentTotalUsageGap[1] > 0)
//		{
//			//both unhealth
//			prob = (float)0.1;	
//			System.out.println("both unhealth");
//		} else 
//		{
//			//for 0 situation
//			prob = (float)0.2;
//			System.out.println("0");
//		}

		
//			if (prob < (float)0.0) {
//				prob = (float)0.0;
//			}
//			if (prob > (float)1.0 && prob != 33.33) {
//				prob = (float)1.0;
//			}
		
		this.actionNumber = -1;//TODO init
		return prob;		
	}


		
		/*
		this.str = new ArrayList<String[]>();
		line = br.readLine();

		// 1行ごとにString[]化、リストstrに追加
		while (true) {
			line = br.readLine();
			if (line == null)
				break;
			sline = line.split(",");
			str.add(sline.clone());
		}
*/
	
	public boolean decideOpponentAttitude(float prob){
		float point = (float) Math.random();
		//TODO DEBUG
		System.out.println("point =" + point);
		System.out.println("prob =" + prob);
				
		if (point < prob && point > 0) {
			return true;
			
		} else {
			return false;
		}

	}
	
	public float changeMCTS(boolean decision, float ucbScore) {
		if (decision) {
			return ucbScore;
		} else  {
			return (float) ((-1.0) * ucbScore);
		}
	}
	//PDA AI function end	
	
	
	
	
}
