package org.uci.spacifyLib.dto;

import lombok.Getter;
import lombok.Setter;
import org.uci.spacifyLib.interfaces.RulesInterface;

@Getter
@Setter
public class Rules extends RulesInterface {

    private int calculatedValue1;
    private int thresholdValue1;
    private int calculatedValue2;
    private int thresholdValue2;
    private int totalCredits = 0;

}
