package com.axelor.base.service.ical;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import javax.mail.MessagingException;

import com.axelor.apps.base.db.ICalendarEvent;
import com.axelor.apps.base.db.ICalendarUser;
import com.axelor.apps.base.ical.ICalendarException;
import com.axelor.apps.message.db.EmailAddress;
import com.axelor.exception.AxelorException;

import net.fortuna.ical4j.model.ValidationException;

public class ICalendarEventServiceImpl implements ICalendarEventService{

	@Override
	public List<ICalendarUser> addEmailGuest(EmailAddress email, ICalendarEvent event) throws ClassNotFoundException, InstantiationException, IllegalAccessException, AxelorException, MessagingException, IOException, ICalendarException, ValidationException, ParseException{
		if(email != null){
			if(event.getAttendees() == null || !event.getAttendees().stream().anyMatch(x -> email.getAddress().equals(x.getEmail()))){
				ICalendarUser calUser = new ICalendarUser();
				calUser.setEmail(email.getAddress());
				calUser.setName(email.getName());
				if(email.getPartner() != null && email.getPartner().getUser() != null){
					calUser.setUser(email.getPartner().getUser());
				}
				event.addAttendee(calUser);
			}
		}
		return event.getAttendees();
	}
}
