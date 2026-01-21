package ru.art.home.market.repositoryes;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import ru.art.home.market.model.Item;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ItemRepositoryTest {

    @Autowired
    private ItemRepository itemRepository;

    @Test
    void saveAndFindAll() {
        long before = itemRepository.count();
        Item item = new Item(null, "Phone", "Desc", "/img.png", 1000L);
        itemRepository.save(item);

        assertThat(itemRepository.count()).isEqualTo(before + 1);
    }
}
