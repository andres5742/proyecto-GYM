package com.gym.management.dto;

import java.util.List;

public record HandoverInventorySnapshot(int unitsTotal, List<HandoverDeliveredProductLine> lines) {}
