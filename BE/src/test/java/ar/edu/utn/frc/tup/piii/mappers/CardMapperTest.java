package ar.edu.utn.frc.tup.piii.mappers;

import ar.edu.utn.frc.tup.piii.dtos.card.AbilityDTO;
import ar.edu.utn.frc.tup.piii.dtos.card.AttackDTO;
import ar.edu.utn.frc.tup.piii.dtos.card.CardDTO;
import ar.edu.utn.frc.tup.piii.entities.CardEntity;
import ar.edu.utn.frc.tup.piii.enums.AbilityType;
import ar.edu.utn.frc.tup.piii.enums.EnergyType;
import ar.edu.utn.frc.tup.piii.enums.PokemonStage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CardMapperTest {

    private CardMapper cardMapper;

    @BeforeEach
    void setUp() {
        cardMapper = new CardMapper(new ObjectMapper());
    }

    @Test
    void toDTO_shouldMapAllFieldsCorrectly() {
        CardEntity entity = buildPokemonEntity();
        CardDTO dto = cardMapper.toDTO(entity);

        assertEquals(entity.getId().toString(), dto.getId());
        assertEquals(entity.getExternalId(), dto.getExternalId());
        assertEquals(entity.getName(), dto.getName());
        assertEquals(entity.getSupertype(), dto.getSupertype());
        assertEquals(entity.getSubtypes(), dto.getSubtypes());
        assertEquals(entity.getHp(), dto.getHp());
        assertEquals(entity.getTypes(), dto.getTypes());
        assertEquals(entity.getStage(), dto.getStage());
        assertEquals(entity.getEvolvesFrom(), dto.getEvolvesFrom());
        assertEquals(entity.getWeaknessType(), dto.getWeaknessType());
        assertEquals(entity.getResistanceType(), dto.getResistanceType());
        assertEquals(entity.getRetreatCost(), dto.getRetreatCost());
        assertEquals(entity.getImageUrl(), dto.getImageUrl());
        assertEquals(entity.getRarity(), dto.getRarity());
        assertEquals(entity.getCardNumber(), dto.getCardNumber());
        assertEquals(entity.getSetId(), dto.getSetId());
    }

    @Test
    void toDTO_shouldReturnEmptyAttacksWhenAttacksJsonIsNull() {
        CardEntity entity = buildPokemonEntity();
        entity.setAttacksJson(null);
        CardDTO dto = cardMapper.toDTO(entity);
        assertNotNull(dto.getAttacks());
        assertTrue(dto.getAttacks().isEmpty());
    }

    @Test
    void toDTO_shouldReturnEmptyAttacksWhenAttacksJsonIsBlank() {
        CardEntity entity = buildPokemonEntity();
        entity.setAttacksJson("   ");
        CardDTO dto = cardMapper.toDTO(entity);
        assertNotNull(dto.getAttacks());
        assertTrue(dto.getAttacks().isEmpty());
    }

    @Test
    void toDTO_shouldReturnEmptyAbilitiesWhenAbilitiesJsonIsNull() {
        CardEntity entity = buildPokemonEntity();
        entity.setAbilitiesJson(null);
        CardDTO dto = cardMapper.toDTO(entity);
        assertNotNull(dto.getAbilities());
        assertTrue(dto.getAbilities().isEmpty());
    }

    @Test
    void toDTO_shouldParseNumericDamage() {
        CardEntity entity = buildPokemonEntity();
        entity.setAttacksJson("[{\"name\":\"Attack\",\"cost\":[\"FIRE\",\"COLORLESS\"],\"damage\":\"60\",\"text\":\"\"}]");
        CardDTO dto = cardMapper.toDTO(entity);

        assertEquals(1, dto.getAttacks().size());
        AttackDTO attack = dto.getAttacks().get(0);
        assertEquals(60, attack.getDamage());
        assertNull(attack.getEffectText());
    }

    @Test
    void toDTO_shouldParseEmptyDamage() {
        CardEntity entity = buildPokemonEntity();
        entity.setAttacksJson("[{\"name\":\"Attack\",\"cost\":[\"COLORLESS\"],\"damage\":\"\",\"text\":\"Discard an Energy\"}]");
        CardDTO dto = cardMapper.toDTO(entity);

        AttackDTO attack = dto.getAttacks().get(0);
        assertEquals(0, attack.getDamage());
        assertEquals("Discard an Energy", attack.getEffectText());
    }

    @Test
    void toDTO_shouldParseDamageWithModifier() {
        CardEntity entity = buildPokemonEntity();
        entity.setAttacksJson("[{\"name\":\"Attack\",\"cost\":[\"FIRE\"],\"damage\":\"120+\",\"text\":\"does more damage\"}]");
        CardDTO dto = cardMapper.toDTO(entity);

        AttackDTO attack = dto.getAttacks().get(0);
        assertEquals(120, attack.getDamage());
        assertEquals("120+ does more damage", attack.getEffectText());
    }

    @Test
    void toDTO_shouldParseMultiplierDamage() {
        CardEntity entity = buildPokemonEntity();
        entity.setAttacksJson("[{\"name\":\"Attack\",\"cost\":[\"COLORLESS\"],\"damage\":\"10\u00d7\",\"text\":\"Flip coins\"}]");
        CardDTO dto = cardMapper.toDTO(entity);

        AttackDTO attack = dto.getAttacks().get(0);
        assertEquals(0, attack.getDamage());
        assertNotNull(attack.getEffectText());
        assertTrue(attack.getEffectText().contains("10\u00d7"));
    }

    @Test
    void toDTO_shouldParseAbilitiesCorrectly() {
        CardEntity entity = buildPokemonEntity();
        entity.setAbilitiesJson("[{\"name\":\"Thick Skin\",\"text\":\"prevent all effects of attacks\",\"type\":\"Ability\"}]");
        CardDTO dto = cardMapper.toDTO(entity);

        assertEquals(1, dto.getAbilities().size());
        AbilityDTO ability = dto.getAbilities().get(0);
        assertEquals("Thick Skin", ability.getName());
        assertEquals("prevent all effects of attacks", ability.getEffectText());
        assertEquals(AbilityType.POKE_BODY, ability.getType());
    }

    @Test
    void toDTO_shouldReturnNullStageForTrainer() {
        CardEntity entity = buildPokemonEntity();
        entity.setSupertype("Trainer");
        entity.setStage(null);
        CardDTO dto = cardMapper.toDTO(entity);
        assertNull(dto.getStage());
    }

    @Test
    void toDTOList_shouldMapMultipleEntities() {
        List<CardEntity> entities = List.of(buildPokemonEntity(), buildEnergyEntity());
        List<CardDTO> dtos = cardMapper.toDTOList(entities);
        assertEquals(2, dtos.size());
    }

    private CardEntity buildPokemonEntity() {
        CardEntity entity = CardEntity.builder()
                .externalId("xy1-1")
                .name("Bulbasaur")
                .supertype("Pokémon")
                .subtypes(List.of("Basic"))
                .hp(60)
                .types(Set.of(EnergyType.GRASS))
                .stage(PokemonStage.BASIC)
                .evolvesFrom(null)
                .weaknessType("FIRE")
                .resistanceType(null)
                .retreatCost(1)
                .imageUrl("https://example.com/bulbasaur.png")
                .rarity("Common")
                .cardNumber("1")
                .setId("xy1")
                .attacksJson("[{\"name\":\"Tackle\",\"cost\":[\"COLORLESS\",\"COLORLESS\"],\"damage\":\"30\",\"text\":\"\"}]")
                .abilitiesJson(null)
                .rules(null)
                .build();
        entity.setId(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        return entity;
    }

    private CardEntity buildEnergyEntity() {
        CardEntity entity = CardEntity.builder()
                .externalId("xy1-energy-grass-001")
                .name("Grass Energy")
                .supertype("Energy")
                .subtypes(List.of("Basic"))
                .hp(null)
                .types(Set.of(EnergyType.GRASS))
                .stage(null)
                .evolvesFrom(null)
                .weaknessType(null)
                .resistanceType(null)
                .retreatCost(null)
                .imageUrl("")
                .rarity(null)
                .cardNumber("001")
                .setId("xy1")
                .attacksJson(null)
                .abilitiesJson(null)
                .rules(null)
                .build();
        entity.setId(UUID.fromString("550e8400-e29b-41d4-a716-446655440001"));
        return entity;
    }
}
