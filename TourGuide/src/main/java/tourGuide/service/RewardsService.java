package tourGuide.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import tourGuide.user.User;
import tourGuide.user.UserReward;

@Service
public class RewardsService {
    private static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;
	private final Logger logger = LoggerFactory.getLogger(RewardsService.class);

	// proximity in miles
    private int defaultProximityBuffer = 100;
	private int proximityBuffer = defaultProximityBuffer;
	private int attractionProximityRange = 200;
	private final GpsUtil gpsUtil;
	private final RewardCentral rewardsCentral;
	private final ExecutorService executorService = Executors.newFixedThreadPool(1000);


	public RewardsService(GpsUtil gpsUtil, RewardCentral rewardCentral) {
		this.gpsUtil = gpsUtil;
		this.rewardsCentral = rewardCentral;
	}
	
	/**
	 * Sets the proximity buffer
	 *
	 * @param proximityBuffer
	 */
	public void setProximityBuffer(int proximityBuffer) {
		this.proximityBuffer = proximityBuffer;
	}
	
	/**
	 * Set the proximity buffer to the default value.
	 */
	public void setDefaultProximityBuffer() {
		proximityBuffer = defaultProximityBuffer;
	}

	/**
	 * For each user location, check if the user is near an attraction. If so, add the reward to the user's reward list
	 *
	 * @param user The user object that we are calculating rewards for.
	 */
	public void calculateRewards(User user) {
		CopyOnWriteArrayList<VisitedLocation> userLocations = new CopyOnWriteArrayList<>(user.getVisitedLocations());
		List<Attraction> attractions = gpsUtil.getAttractions();

		CopyOnWriteArrayList<CompletableFuture> futures = new CopyOnWriteArrayList<>();

		for(VisitedLocation visitedLocation : userLocations) {
			for (Attraction attr : attractions) {
				futures.add(
						CompletableFuture.runAsync(()-> {
							if(user.getUserRewards().stream().filter(r -> r.attraction.attractionName.equals(attr.attractionName)).count() == 0) {
								if(nearAttraction(visitedLocation, attr)) {
									user.addUserReward( new UserReward(visitedLocation, attr,  rewardsCentral.getAttractionRewardPoints(attr.attractionId, user.getUserId())));
								}
							}
						},executorService)
				);
			}
		}

		futures.forEach((n)-> {
			try {
				n.get();
			} catch (InterruptedException e) {
				logger.error("Calculate Rewards InterruptedException: " + e);
			} catch (ExecutionException e) {
				logger.error("Calculate Rewards ExecutionException: " + e);
			} finally {
				futures.remove(n);
			}
		});
	}

	/**
	 * If the distance between the attraction and the location is greater than the attraction proximity range, return false,
	 * otherwise return true.
	 *
	 * @param attraction The attraction object that we want to check if it's within the proximity range.
	 * @param location The location of the user
	 * @return A boolean value.
	 */
	public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
		return getDistance(attraction, location) > attractionProximityRange ? false : true;
	}
	
	/**
	 * > If the distance between the attraction and the visited location is greater than the proximity buffer, return false,
	 * otherwise return true
	 *
	 * @param visitedLocation The location that the user is currently at.
	 * @param attraction The attraction that we're checking to see if we're near.
	 * @return A boolean value.
	 */
	private boolean nearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
		return getDistance(attraction, visitedLocation.location) >= proximityBuffer ? false : true;
	}
	
	/**
	 * > Get rewards points for user name : {user.getUserName()} and attraction name {attraction.attractionId}
	 *
	 * @param attraction The attraction object that the user is visiting.
	 * @param user The user object that is passed in from the calling method.
	 * @return The number of reward points the user has for the attraction.
	 */
	public int getRewardPoints(Attraction attraction, User user) {
//		logger.info("Get rewards points for user name : {} and attraction name ", user.getUserName());
		return rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId());
	}
	
	public double getDistance(Location loc1, Location loc2) {
        double lat1 = Math.toRadians(loc1.latitude);
        double lon1 = Math.toRadians(loc1.longitude);
        double lat2 = Math.toRadians(loc2.latitude);
        double lon2 = Math.toRadians(loc2.longitude);

        double angle = Math.acos(Math.sin(lat1) * Math.sin(lat2)
                               + Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2));

        double nauticalMiles = 60 * Math.toDegrees(angle);
        double statuteMiles = STATUTE_MILES_PER_NAUTICAL_MILE * nauticalMiles;
        return statuteMiles;
	}

}
