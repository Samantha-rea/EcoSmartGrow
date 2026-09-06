package com.ecosmartgrow.repository;

import android.content.Context;

import com.ecosmartgrow.database.PlantProfileDAO;
import com.ecosmartgrow.model.PlantProfile;

import java.util.List;

public class PlantRepository {
    private PlantProfileDAO plantProfileDAO;

    public PlantRepository(Context context) {
        this.plantProfileDAO = new PlantProfileDAO(context);
    }

    public long savePlant(PlantProfile plant) {
        return plantProfileDAO.insertPlantProfile(plant);
    }

    public List<PlantProfile> getAllPlants() {
        return plantProfileDAO.getAllPlants();
    }

    public PlantProfile getPlantById(int id) {
        return plantProfileDAO.getPlantById(id);
    }

    public PlantProfile getPlantByName(String name) {
        return plantProfileDAO.getPlantByName(name);
    }

    public int updatePlant(PlantProfile plant) {
        return plantProfileDAO.updatePlantProfile(plant);
    }

    public int deletePlant(int id) {
        return plantProfileDAO.deletePlantProfile(id);
    }
}