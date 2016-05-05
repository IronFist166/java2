package hu.sol.mik.book.vaadin;

import hu.sol.mik.book.bean.Book;
import hu.sol.mik.book.dao.BookDao;
import hu.sol.mik.book.dao.impl.BookDaoImpl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.vaadin.data.Item;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.fieldgroup.BeanFieldGroup;
import com.vaadin.data.fieldgroup.FieldGroup.CommitException;
import com.vaadin.data.util.BeanContainer;
import com.vaadin.data.util.BeanItem;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnGenerator;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

public class BookUI extends UI {

	private BeanContainer<Long, Book> bookDataSource;
	private BookDao bookDao;
	protected Window editBookWindow;
	protected Set<Book> selectedBooks;

	@Override
	protected void init(VaadinRequest request) {
		bookDao = new BookDaoImpl();
		this.setContent(createBookVerticalLayout());
	}

	private Component createBookVerticalLayout() {
		VerticalLayout verticalLayout = new VerticalLayout();
		verticalLayout.setSpacing(true);
		verticalLayout.setMargin(true);
		verticalLayout.addComponent(createBookTable());
		verticalLayout.addComponent(createFunctionLayout());
		return verticalLayout;
	}

	private Component createFunctionLayout() {
		HorizontalLayout horizontalLayout = new HorizontalLayout();
		Button newBookButton              = new Button("Új könyv felvitel");
		Button deleteSelectedBooksButton  = new Button("Kiválasztottak törlése");
		
		horizontalLayout.setSpacing(true);
		horizontalLayout.setMargin(true);
		
		newBookButton.addClickListener(new ClickListener() {

			@Override
			public void buttonClick(ClickEvent event) {
				Book book = new Book();
				openEditWindw(book, "Új könyv felvitel");
			}

		});
		
		deleteSelectedBooksButton.addClickListener(new ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				Iterator<Book> iterator = selectedBooks.iterator();
				while(iterator.hasNext()) {
					bookDao.delete(bookDataSource.getItem(iterator.next()).getBean());
				}
				
				refreshTable();
				Notification.show("Sikeres törlés");
			}
		});
		
		horizontalLayout.addComponent(newBookButton);
		horizontalLayout.addComponent(deleteSelectedBooksButton);
		
		return horizontalLayout;
	}

	private void openEditWindw(Book book, String title) {
		editBookWindow = new Window(title);
		editBookWindow.setHeight("90%");
		editBookWindow.setWidth("90%");
		editBookWindow.center();
		editBookWindow.setContent(createBookEditLayout(book));
		this.addWindow(editBookWindow);
	}

	protected Component createBookEditLayout(Book book) {
		VerticalLayout verticalLayout = new VerticalLayout();
		final BeanFieldGroup<Book> bookBinder = new BeanFieldGroup<Book>(
				Book.class);
		bookBinder.setItemDataSource(book);
		verticalLayout.addComponent(createBookEditForm(bookBinder));
		Button saveButton = new Button("Mentés");
		saveButton.addClickListener(new ClickListener() {

			@Override
			public void buttonClick(ClickEvent event) {
				try {
					bookBinder.commit();
					Book bean = bookBinder.getItemDataSource().getBean();
					if (bean.getId() == null) {
						bookDao.saveBook(bean);
					} else {
						bookDao.updateBook(bean);
					}
					editBookWindow.close();
					Notification.show("Sikeres felvitel");
					refreshTable();
				} catch (CommitException e) {
					Notification.show("Hiba történt");
				}
			}

		});
		verticalLayout.addComponent(saveButton);
		return verticalLayout;
	}

	private void refreshTable() {
		bookDataSource.removeAllItems();
		bookDataSource.addAll(bookDao.listAll());
	}

	private Component createBookEditForm(BeanFieldGroup<Book> bookBinder) {
		FormLayout formLayout = new FormLayout();
		TextField titleField = bookBinder.buildAndBind("Cím", "title", TextField.class);
		TextField descField = bookBinder.buildAndBind("Leírás", "description", TextField.class);
		TextField authorField = bookBinder.buildAndBind("Szerző", "author", TextField.class);
		TextField pubYearField = bookBinder.buildAndBind("Publikáció", "pubYear", TextField.class);
		
		titleField.setNullRepresentation("");
		descField.setNullRepresentation("");
		authorField.setNullRepresentation("");
		
		formLayout.addComponent(titleField);
		formLayout.addComponent(descField);
		formLayout.addComponent(authorField);
		formLayout.addComponent(pubYearField);
		
		return formLayout;
	}

	private Component createBookTable() {
		final Table table = new Table("Könyvek listája");
		table.setSizeFull();
		bookDataSource = new BeanContainer<Long, Book>(Book.class);
		bookDataSource.setBeanIdProperty("id");
		bookDataSource.addAll(bookDao.listAll());
		table.setContainerDataSource(bookDataSource);
		
		table.setVisibleColumns("title", "description", "author", "pubYear");
		table.addGeneratedColumn("editBook", new ColumnGenerator() {

			@Override
			public Object generateCell(final Table source, final Object itemId, Object columnId) {
				Button button = new Button("Szerkeszt");
				button.addClickListener(new ClickListener() {

					@Override
					public void buttonClick(ClickEvent event) {
						BeanItem<Book> beanItem = ((BeanItem<Book>) source.getItem(itemId));
						openEditWindw(beanItem.getBean(), "Könyv szerkesztése");
					}
				});
				return button;
			}
		});
		
		table.setColumnHeader("title", "Cím");
		table.setColumnHeader("description", "Leírás");
		table.setColumnHeader("author", "Szerző");
		table.setColumnHeader("pubYear", "Megjelenés éve");
		table.setColumnHeader("editBook", "Szerkesztés");
		
		table.setSelectable(true);
		table.setMultiSelect(true);
		table.setNullSelectionAllowed(true);
		table.addValueChangeListener(new ValueChangeListener() {
			@Override
			public void valueChange(ValueChangeEvent event) {
				selectedBooks = (Set<Book>)table.getValue();
			}
		});
		
		return table;
	}
}