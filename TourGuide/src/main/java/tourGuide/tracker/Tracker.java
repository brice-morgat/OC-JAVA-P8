package tourGuide.tracker;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tourGuide.service.TourGuideService;
import tourGuide.user.User;

public class Tracker extends Thread {
	private Logger logger = LoggerFactory.getLogger(Tracker.class);
	private static final long trackingPollingInterval = TimeUnit.MINUTES.toSeconds(5);
	private final ExecutorService executorService = Executors.newSingleThreadExecutor();
	private final TourGuideService tourGuideService;
	private boolean stop = false;
	private final Map<User, Boolean> completedTrackingMap = new HashMap<>();

	public Tracker(TourGuideService tourGuideService) {
		this.tourGuideService = tourGuideService;
		
		executorService.submit(this);
	}
	
	/**
	 * Assures to shut down the Tracker thread
	 */
	public void stopTracking() {
		stop = true;
		executorService.shutdownNow();
	}

	public synchronized void finalizeTrack(User user) {
		completedTrackingMap.put(user, true);
	}
	
	@Override
	public void run() {
		StopWatch stopWatch = new StopWatch();
		while(true) {

			if(Thread.currentThread().isInterrupted() || stop) {
				logger.debug("Shutting down tracker");
				break;
			}

			List<User> users = tourGuideService.getAllUsers();
			users.forEach(u -> completedTrackingMap.put(u, false));

			logger.debug("Begin Tracker. Tracking " + users.size() + " users.");
			stopWatch.start();
			users.forEach(u -> tourGuideService.trackUserLocation(u));

			boolean notFinished = true;
			while(notFinished) {
				try {
					//logger.debug("Waiting for tracking to finish...");
					TimeUnit.MILLISECONDS.sleep(100);
				} catch (InterruptedException e) {
					break;
				}

				if(!completedTrackingMap.containsValue(false)) {
					notFinished = false;
				}
			}

			completedTrackingMap.clear();

			stopWatch.stop();
			logger.debug("Tracker Time Elapsed: " + TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()) + " seconds.");
			stopWatch.reset();

			try {
				logger.debug("Tracker sleeping");
				TimeUnit.SECONDS.sleep(trackingPollingInterval);
			} catch (InterruptedException e) {
				break;
			}
		}
	}
}
