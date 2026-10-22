package ar.edu.utn.frc.tup.piii.mappers;

import ar.edu.utn.frc.tup.piii.clients.PokemonTcgApiClient;
import ar.edu.utn.frc.tup.piii.clients.dtos.AbilityApiDTO;
import ar.edu.utn.frc.tup.piii.clients.dtos.AttackApiDTO;
import ar.edu.utn.frc.tup.piii.dtos.card.AbilityDTO;
import ar.edu.utn.frc.tup.piii.dtos.card.AttackDTO;
import ar.edu.utn.frc.tup.piii.dtos.card.CardDTO;
import ar.edu.utn.frc.tup.piii.entities.CardEntity;
import ar.edu.utn.frc.tup.piii.enums.AbilityType;
import ar.edu.utn.frc.tup.piii.enums.EnergyType;
import ar.edu.utn.frc.tup.piii.enums.PokemonStage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CardMapper {

    private static final Logger log = LoggerFactory.getLogger(CardMapper.class);

    private final ObjectMapper objectMapper;

    public CardMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public CardDTO toDTO(CardEntity entity) {
        if (entity == null) return null;
        CardDTO dto = new CardDTO();
        dto.setId(entity.getId() != null ? entity.getId().toString() : null);
        dto.setExternalId(entity.getExternalId());
        dto.setName(entity.getName());
        dto.setSupertype(entity.getSupertype());
        dto.setSubtypes(entity.getSubtypes());
        dto.setHp(entity.getHp());
        dto.setTypes(entity.getTypes());
        dto.setStage(entity.getStage());
        dto.setEvolvesFrom(entity.getEvolvesFrom());
        dto.setAttacks(parseAttacksFromJson(entity.getAttacksJson()));
        dto.setAbilities(parseAbilitiesFromJson(entity.getAbilitiesJson()));
        dto.setWeaknessType(entity.getWeaknessType());
        dto.setResistanceType(entity.getResistanceType());
        dto.setRetreatCost(entity.getRetreatCost());
        dto.setImageUrl(entity.getImageUrl());
        dto.setRarity(entity.getRarity());
        dto.setCardNumber(entity.getCardNumber());
        dto.setSetId(entity.getSetId());
        dto.setRules(parseStringListFromJson(entity.getRules()));
        return dto;
    }

    public List<CardDTO> toDTOList(List<CardEntity> entities) {
        if (entities == null) return Collections.emptyList();
        return entities.stream().map(this::toDTO).toList();
    }

    public CardEntity toEntity(PokemonTcgApiClient.PokemonTcgCardResponse api) {
        if (api == null) return null;
        CardEntity entity = new CardEntity();
        entity.setExternalId(api.getId());
        entity.setName(api.getName());
        entity.setSupertype(api.getSupertype());
        entity.setSubtypes(api.getSubtypes());
        entity.setHp(parseHp(api.getHp()));
        entity.setTypes(parseEnergyTypes(api.getTypes()));
        entity.setStage(deriveStage(api.getSubtypes(), api.getSupertype()));
        entity.setEvolvesFrom(api.getEvolvesFrom());
        entity.setAttacksJson(writeJson(api.getAttacks()));
        entity.setAbilitiesJson(writeJson(api.getAbilities()));
        if (api.getWeaknesses() != null && !api.getWeaknesses().isEmpty()) {
            entity.setWeaknessType(api.getWeaknesses().get(0).getType());
        }
        if (api.getResistances() != null && !api.getResistances().isEmpty()) {
            entity.setResistanceType(api.getResistances().get(0).getType());
        }
        entity.setRetreatCost(api.getConvertedRetreatCost());
        entity.setImageUrl(api.getImages() != null && api.getImages().getLarge() != null ? api.getImages().getLarge() : "");
        entity.setRules(writeJson(api.getRules()));
        entity.setRarity(api.getRarity());
        entity.setCardNumber(api.getNumber());
        entity.setSetId(api.getSet() != null ? api.getSet().getId() : "xy1");
        return entity;
    }

    public List<CardEntity> toEntityList(List<PokemonTcgApiClient.PokemonTcgCardResponse> apiList) {
        if (apiList == null) return Collections.emptyList();
        return apiList.stream().map(this::toEntity).toList();
    }

    AttackDTO parseDamage(AttackApiDTO api) {
        AttackDTO dto = new AttackDTO();
        dto.setName(api.getName());
        if (api.getCost() != null) {
            dto.setCost(api.getCost().stream()
                    .map(this::safeEnergyType)
                    .filter(Objects::nonNull)
                    .toList());
        }
        String damageStr = api.getDamage();
        String text = api.getText();
        if (damageStr == null || damageStr.isBlank()) {
            dto.setDamage(0);
            dto.setEffectText(text != null && !text.isBlank() ? text : null);
        } else if (damageStr.endsWith("+")) {
            try {
                dto.setDamage(Integer.parseInt(damageStr.replace("+", "")));
                dto.setEffectText(text != null && !text.isBlank() ? damageStr + " " + text : damageStr);
            } catch (NumberFormatException e) {
                dto.setDamage(0);
                dto.setEffectText(text != null ? damageStr + " " + text : damageStr);
            }
        } else if (damageStr.endsWith("×") || damageStr.endsWith("x")) {
            dto.setDamage(0);
            String suffix = text != null ? damageStr + " " + text : damageStr;
            dto.setEffectText(suffix);
        } else {
            try {
                dto.setDamage(Integer.parseInt(damageStr));
                dto.setEffectText(text != null && !text.isBlank() ? text : null);
            } catch (NumberFormatException e) {
                dto.setDamage(0);
                dto.setEffectText(text != null ? damageStr + " " + text : damageStr);
            }
        }
        return dto;
    }

    PokemonStage deriveStage(List<String> subtypes, String supertype) {
        if (!"Pokémon".equals(supertype) || subtypes == null) return null;
        if (subtypes.contains("MEGA")) return PokemonStage.MEGA;
        if (subtypes.contains("EX")) return PokemonStage.EX;
        if (subtypes.contains("Stage 2")) return PokemonStage.STAGE2;
        if (subtypes.contains("Stage 1")) return PokemonStage.STAGE1;
        if (subtypes.contains("Basic")) return PokemonStage.BASIC;
        return null;
    }

    private List<AttackDTO> parseAttacksFromJson(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            List<AttackApiDTO> apiList = objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, AttackApiDTO.class));
            return apiList.stream().map(this::parseDamage).toList();
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }

    private List<AbilityDTO> parseAbilitiesFromJson(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            List<AbilityApiDTO> apiList = objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, AbilityApiDTO.class));
            return apiList.stream().map(a -> {
                AbilityDTO dto = new AbilityDTO();
                dto.setName(a.getName());
                dto.setType(resolveAbilityType(a.getType(), a.getText()));
                dto.setEffectText(a.getText());
                return dto;
            }).toList();
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }

    AbilityType resolveAbilityType(String apiType, String effectText) {
        if ("POKEMON_POWER".equals(apiType)) return AbilityType.POKE_POWER;
        if ("POKE_BODY".equals(apiType)) return AbilityType.POKE_BODY;
        if ("Ability".equals(apiType)) {
            String lower = effectText != null ? effectText.toLowerCase() : "";
            if (lower.contains("during your turn")) return AbilityType.POKE_POWER;
            return AbilityType.POKE_BODY;
        }
        return AbilityType.POKE_BODY;
    }

    private List<String> parseStringListFromJson(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }

    private Integer parseHp(String hp) {
        if (hp == null || hp.isBlank()) return null;
        try {
            return Integer.parseInt(hp.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String writeJson(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private Set<EnergyType> parseEnergyTypes(List<String> types) {
        if (types == null) return Collections.emptySet();
        return types.stream()
                .map(this::safeEnergyType)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private EnergyType safeEnergyType(String value) {
        try {
            return EnergyType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown energy type: '{}', skipping", value);
            return null;
        }
    }
}
