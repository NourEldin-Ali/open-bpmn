package org.openbpmn.discovery.loop;

import org.junit.jupiter.api.Test;
import org.openbpmn.bpmn.discovery.BPMNDiscovery;
import org.openbpmn.bpmn.discovery.compare.BPMNComparatorExecutor;
import org.openbpmn.bpmn.discovery.model.*;
import org.openbpmn.bpmn.exceptions.BPMNModelException;

import java.util.*;
import java.util.logging.Logger;

public class test {
	private static Logger logger = Logger.getLogger(test.class.getName());

	/**
	 * This test creates an empty BPMN model instance
	 * 
	 * @throws BPMNModelException
	 * @throws CloneNotSupportedException 
	 */
	@Test
	public void testInputProcess() throws BPMNModelException, CloneNotSupportedException {
		logger.info("testing diagram Start generating model");
		String path = "src/test/resources/discovery/loop/testing.bpmn";
		LinkedList<String> list = new LinkedList<>();
		List<String> startsEvent = new ArrayList<>();
		Set<Set<String>> parallelRelations = new HashSet<>();
		DependencyGraph bpmnTransformation = new DependencyGraph();
		Map<String, Map<String, String>> elementsInfo = new HashMap<>();
		List<String> endEvents = new LinkedList<>();
		Map<String,String> elementsName = new HashMap<>();


		// start event
		String startEvent = "start";
		startsEvent.add(startEvent);

		// dependencies
		list.add("start->a");
		list.add("a->b");
		list.add("a->c");
		list.add("a->d");

		list.add("b->b");
		list.add("b->c");
//		list.add("b->d");

		list.add("c->b");
		list.add("c->c");
//		list.add("c->d");

		list.add("d->b");
		list.add("d->c");
//		list.add("d->d");


		list.add("b->h");
		list.add("c->h");
		list.add("d->h");

		list.add("h->end");


		// parallelism
		parallelRelations.add(new HashSet<String>() {{
			add("b"); add("c");
		}});
		parallelRelations.add(new HashSet<String>() {{
			add("c"); add("d");
		}});
		parallelRelations.add(new HashSet<String>() {{
			add("b"); add("d");
		}});




		// elements info
		// start/end/human/service

		elementsInfo.put(startEvent, new HashMap<String, String>() {{ put("type", "start"); }});
		elementsInfo.put("end", new HashMap<String, String>() {{ put("type", "end"); }});



		// extract dependencies
		for (String dependency : list) {
			// Split the dependency string into source and target
			String[] parts = dependency.split("->");

			// Extract source and target
			String sourceId = parts[0].trim();
			String targetId = parts[1].trim();
			elementsName.put(sourceId, parts[0]);
			elementsName.put(targetId, parts[1]);
			bpmnTransformation.addVertex(sourceId);
			bpmnTransformation.addVertex(targetId);
			bpmnTransformation.addEdge(sourceId, targetId);
			
		}

		// extract end events
		for (Map.Entry<String, Map<String, String>> entry : elementsInfo.entrySet()) {
			if (entry.getValue().get("type").contentEquals("end")) {
				endEvents.add(entry.getKey());
			}
		}
		bpmnTransformation.endActivities = endEvents;
		bpmnTransformation.startActivities = startsEvent;
		bpmnTransformation.parallelism = parallelRelations;
		bpmnTransformation.elementInformations = elementsInfo;
		bpmnTransformation.elementsName = elementsName;
		// find loops
		bpmnTransformation.findAndRemoveLoops();
//		bpmnTransformation.removeParallelism();
		bpmnTransformation.findInclusive();
		bpmnTransformation.findExculisve();



//		System.out.println(bpmnTransformation.loops);
//		System.out.println(bpmnTransformation.getLoops());

		LoopMerger loopMerger = new LoopMerger(bpmnTransformation.loops, bpmnTransformation.dependencyGraphWithLoop);
		System.out.println(loopMerger.getMergedLoop());

		System.out.println("exlusive");
		System.out.println(bpmnTransformation.exlusive);

		DecisionForLoop decisionForLoop = new DecisionForLoop(bpmnTransformation.exlusive, bpmnTransformation.parallelism,
				bpmnTransformation.inclusive, loopMerger.getMergedLoop());
		decisionForLoop.appendDecisions();

		System.out.println("exclusive");
		System.out.println(bpmnTransformation.exlusive);

		//get exclusive
		DecisionMerger decisionMerger = new DecisionMerger(bpmnTransformation.exlusive, bpmnTransformation.dependencyGraph);
		LinkedList<LinkedList<String>> decisionRelations = decisionMerger.getDecisions();

		System.out.println("decisionRelations");
		System.out.println(decisionRelations);

		//get parallelism
		ParallelismMerger parallelismMerger = new ParallelismMerger(bpmnTransformation.parallelism,
				bpmnTransformation.dependencyGraphWithLoop);
		LinkedList<LinkedList<String>> parallelMergeRelations = parallelismMerger.getParallelims();
		System.out.println("parallelMergeRelations");
		System.out.println(parallelMergeRelations);

		//get inclusive
		ParallelismMerger inclusiveMerger = new ParallelismMerger(bpmnTransformation.inclusive,
				bpmnTransformation.dependencyGraphWithLoop);
		LinkedList<LinkedList<String>> inclusiveRelations = inclusiveMerger.getParallelims();


		Map<String, LinkedList<LinkedList<String>>> relations = new LinkedHashMap<>();
		relations.put(BPMNDiscovery.DECISION, decisionRelations);
		relations.put(BPMNDiscovery.PARALLEL, parallelMergeRelations);
		relations.put(BPMNDiscovery.INCLUSIVE, inclusiveRelations);

//		System.out.println(relations);



		BPMNDiscovery bpmnDiscovery = new BPMNDiscovery(bpmnTransformation);
		bpmnDiscovery.DependencyGraphToBPMN();
		bpmnDiscovery.saveMode(path);




		//compaire the two models
		boolean results = BPMNComparatorExecutor.execute(path, "src/main/resources/discovery/loop/s13.bpmn");
		if(!results){
			logger.warning("s13.bpmn: The two models are not equivalent");
		}else{
			logger.info("s13.bpmn done: The two models are equivalent");
		}

		
	}
}