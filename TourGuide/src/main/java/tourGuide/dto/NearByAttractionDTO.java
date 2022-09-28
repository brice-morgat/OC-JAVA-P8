package tourGuide.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class NearByAttractionDTO {
    private String attractionName;
    private String attractionLocation;
    private Double distance;
    private int rewardPoints;
}
