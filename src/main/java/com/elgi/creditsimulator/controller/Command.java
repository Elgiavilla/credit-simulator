package com.elgi.creditsimulator.controller;

public interface Command {

    String name();
    String description();
    CommandOutcome execute();
}
