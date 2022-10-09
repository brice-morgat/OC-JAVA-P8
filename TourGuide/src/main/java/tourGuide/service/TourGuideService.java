package tourGuide.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import tourGuide.dto.NearByAttractionDTO;
import tourGuide.helper.InternalTestHelper;
import tourGuide.tracker.Tracker;
import tourGuide.user.User;
import tourGuide.user.UserPreferences;
import tourGuide.user.UserReward;
import tripPricer.Provider;
import tripPricer.TripPricer;

import javax.validation.NoProviderFoundException;

@Service
public class TourGuideService {
	private Logger logger = LoggerFactory.getLogger(TourGuideService.class);
	private final GpsUtil gpsUtil;
	private final RewardsService rewardsService;
	private final TripPricer tripPricer = new TripPricer();
	public final Tracker tracker;
	private ExecutorService executorService = Executors.newFixedThreadPool(10000);

	boolean testMode = true;
	
	public TourGuideService(GpsUtil gpsUtil, RewardsService rewardsService) {
		this.gpsUtil = gpsUtil;
		this.rewardsService = rewardsService;
		
		if(testMode) {
			logger.info("TestMode enabled");
			logger.debug("Initializing users");
			initializeInternalUsers();
			logger.debug("Finished initializing users");
		}
		tracker = new Tracker(this);
		addShutDownHook();
	}
	
	/**
	 * Get all the rewards for a user.
	 *
	 * @param user The user object that is being passed in from the controller.
	 * @return A list of UserReward objects.
	 */
	public List<UserReward> getUserRewards(User user) {
		return user.getUserRewards();
	}
	
	/**
	 * "If the user has visited locations, return the last one, otherwise track the user's location and return it."
	 *
	 * The `trackUserLocation` function is asynchronous, so we need to use the `get` method to wait for the result
	 *
	 * @param user The user object that we want to track.
	 * @return A VisitedLocation object.
	 */
	public VisitedLocation getUserLocation(User user) throws ExecutionException, InterruptedException {
		VisitedLocation visitedLocation = (user.getVisitedLocations().size() > 0) ?
			user.getLastVisitedLocation() :
			trackUserLocation(user).get();
		return visitedLocation;
	}
	
	public User getUser(String userName) {
		return internalUserMap.get(userName);
	}


	/**
	 * Get the user with the given userId from the list of all users.
	 *
	 * @param userId The userId of the user you want to get.
	 * @return A User object
	 */
	public User getUserById(UUID userId) {
		User result = null;
		for (User user : getAllUsers()) {
			if (user.getUserId() == userId) {
				result =  user;
			}
		}
		return result;
	}
	
	/**
	 * Return a list of all users in the system.
	 *
	 * @return A list of all the users in the internalUserMap.
	 */
	public List<User> getAllUsers() {
		return internalUserMap.values().stream().collect(Collectors.toList());
	}
	

	/**
	 * If the user doesn't exist, add it to the map.
	 *
	 * @param user The user object to be added to the internal map.
	 */
	public void addUser(User user) {
		if(!internalUserMap.containsKey(user.getUserName())) {
			internalUserMap.put(user.getUserName(), user);
		}
	}
	
	/**
	 * > The function gets the user's trip deals by calling the tripPricer's getPrice function with the user's id, number of
	 * adults, number of children, trip duration, and cumulatative reward points
	 *
	 * @param user The user object that contains the user's preferences and rewards.
	 * @return A list of providers.
	 */
	public List<Provider> getTripDeals(User user) {
		int cumulatativeRewardPoints = user.getUserRewards().stream().mapToInt(i -> i.getRewardPoints()).sum();
		List<Provider> providers = tripPricer.getPrice(tripPricerApiKey, user.getUserId(), user.getUserPreferences().getNumberOfAdults(), 
				user.getUserPreferences().getNumberOfChildren(), user.getUserPreferences().getTripDuration(), cumulatativeRewardPoints);
		user.setTripDeals(providers);
		return providers;
	}

	/**
	 * Update the user preferences for the given user.
	 *
	 * @param user The user object that is being updated.
	 * @param userPreferences The user preferences object that you want to update.
	 * @return The userPreferences object that was just updated.
	 */
	public UserPreferences updateUserPreferences(User user, UserPreferences userPreferences) {
		user.setUserPreferences(userPreferences);
		return user.getUserPreferences();
	}

	/**
	 * > Track Location for user
	 *
	 * @param user The user object that we want to track.
	 * @return CompletableFuture<VisitedLocation>
	 */
	public CompletableFuture<VisitedLocation> trackUserLocation(User user) {
		logger.info("Track Location for user {}", user.getUserName());
		return CompletableFuture.supplyAsync(() -> gpsUtil.getUserLocation(user.getUserId()), executorService)
				.thenApply(visitedLocation -> {
					user.addToVisitedLocations(visitedLocation);
					rewardsService.calculateRewards(user);
					return visitedLocation;
				});
	}

	/**
	 * > Get the top 5 attractions that are closest to the given location
	 *
	 * @param visitedLocation The location that the user has visited.
	 * @return A list of attractions that are nearby the visited location.
	 */
	public List<Attraction> getNearByAttractions(VisitedLocation visitedLocation) {
		List<Attraction> nearbyAttractions = new ArrayList<>();
		List<Attraction> attractions = gpsUtil.getAttractions();
		attractions
				.stream()
				.sorted(Comparator.comparingDouble(attraction -> rewardsService.getDistance(new Location(attraction.latitude, attraction.longitude), visitedLocation.location)))
				.limit(5)
				.forEach(nearbyAttractions::add);
		logger.info(attractions.toString());
		return nearbyAttractions;
	}

	/**
	 * It gets the list of attractions near the visited location, and then for each attraction, it creates a
	 * NearByAttractionDTO object with the attraction name, attraction location, distance from the visited location, and the
	 * reward points for the attraction
	 *
	 * @param visitedLocation The location where the user is currently present.
	 * @return List of NearByAttractionDTO
	 */
	public List<NearByAttractionDTO> getNearByAttractionsList(VisitedLocation visitedLocation) {
		List<Attraction> nearByAttractions = getNearByAttractions(visitedLocation);
		List<NearByAttractionDTO> nearByAttractionDTOList = new ArrayList<>();
		for (Attraction attraction: nearByAttractions) {
			NearByAttractionDTO nearByAttractionDTO = NearByAttractionDTO.builder()
					.attractionName(attraction.attractionName)
					.attractionLocation("Latitude : " + attraction.latitude + ", Longitude: " + attraction.longitude)
					.distance(rewardsService.getDistance(visitedLocation.location, new Location(attraction.latitude, attraction.longitude)))
					.rewardPoints(rewardsService.getRewardPoints(attraction, getUserById(visitedLocation.userId)))
					.build();
			nearByAttractionDTOList.add(nearByAttractionDTO);
		}
		return nearByAttractionDTOList;
	}

	/**
	 * > Get all the users, get the location of each user, and put the user id and location into a map
	 *
	 * @return A map of all the users and their last visited location.
	 */
	public Map<String, Location> getAllCurrentLocation() {
		List<User> allUser = getAllUsers();
		Map<String, Location> allLocation = new HashMap<>();
		for (User user : allUser) {
			String id = user.getUserId().toString();
			Location location = user.getLastVisitedLocation().location;
			allLocation.put(id, location);
		}
		return allLocation;
	}
	
	private void addShutDownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() { 
		      public void run() {
		        tracker.stopTracking();
		      } 
		    }); 
	}
	
	/**********************************************************************************
	 * 
	 * Methods Below: For Internal Testing
	 * 
	 **********************************************************************************/
	private static final String tripPricerApiKey = "test-server-api-key";
	// Database connection will be used for external users, but for testing purposes internal users are provided and stored in memory
	private final Map<String, User> internalUserMap = new HashMap<>();
	private void initializeInternalUsers() {
		IntStream.range(0, InternalTestHelper.getInternalUserNumber()).forEach(i -> {
			String userName = "internalUser" + i;
			String phone = "000";
			String email = userName + "@tourGuide.com";
			User user = new User(UUID.randomUUID(), userName, phone, email);
			generateUserLocationHistory(user);
			
			internalUserMap.put(userName, user);
		});
		logger.debug("Created " + InternalTestHelper.getInternalUserNumber() + " internal test users.");
	}
	
	private void generateUserLocationHistory(User user) {
		IntStream.range(0, 3).forEach(i-> {
			user.addToVisitedLocations(new VisitedLocation(user.getUserId(), new Location(generateRandomLatitude(), generateRandomLongitude()), getRandomTime()));
		});
	}
	
	private double generateRandomLongitude() {
		double leftLimit = -180;
	    double rightLimit = 180;
	    return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}
	
	private double generateRandomLatitude() {
		double leftLimit = -85.05112878;
	    double rightLimit = 85.05112878;
	    return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}
	
	private Date getRandomTime() {
		LocalDateTime localDateTime = LocalDateTime.now().minusDays(new Random().nextInt(30));
	    return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
	}
	
}
