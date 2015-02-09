package org.micromanager.display.internal.link;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.micromanager.display.DisplayWindow;

import org.micromanager.internal.utils.ReportingUtils;

/**
 * This class is for setting up links across DisplayWindows for specific
 * attributes of the DisplaySettings. Extensions of this class will be specific
 * to certain types of the DisplaySettings.
 */
public abstract class SettingsLinker {
   protected DisplayWindow parent_;
   private HashSet<SettingsLinker> linkedLinkers_;
   private List<Class<?>> relevantEvents_;
   private LinkButton button_;
   private boolean isActive_;

   public SettingsLinker(DisplayWindow parent,
         List<Class<?>> relevantEventClasses) {
      parent_ = parent;
      isActive_ = false;
      linkedLinkers_ = new HashSet<SettingsLinker>();
      relevantEvents_ = relevantEventClasses;
   }

   /**
    * NOTE: this must be called before the button is interacted with!
    * Preferably in the LinkButton constructor.
    */
   public void setButton(LinkButton button) {
      button_ = button;
   }

   /**
    * Establish a connection with another SettingsLinker. The connection
    * is reciprocal (i.e. we will call linker.link() in this method).
    */
   public void link(SettingsLinker linker) {
      // Don't link ourselves; just avoids some redundant pushing in
      // pushChanges().
      if (linker != this && !linkedLinkers_.contains(linker)) {
         linkedLinkers_.add(linker);
         linker.link(this);
      }
   }

   /**
    * Remove a connection with another SettingsLinker, and also remove the
    * reciprocal connection.
    */
   public void unlink(SettingsLinker linker) {
      if (linkedLinkers_.contains(linker)) {
         linkedLinkers_.remove(linker);
         linker.unlink(this);
      }
   }

   /**
    * Turn on propagation of events for this linker, so that when our parent's
    * DisplaySettings change, we will push updates to our connected
    * SettingsLinkers.
    */
   public void setIsActive(boolean isActive) {
      if (isActive == isActive_) {
         return;
      }
      isActive_ = isActive;
      button_.setSelected(isActive);
      // Ensure that our linked linkers also get updated.
      for (SettingsLinker linker : linkedLinkers_) {
         if (linker.getIsActive() != isActive_) {
            linker.setIsActive(isActive_);
         }
      }
   }

   /**
    * Return whether or not we should apply changes, that originate from our
    * display, to other displays.
    */
   public boolean getIsActive() {
      return isActive_;
   }

   /**
    * Push the provided event to the linkers we are connected to -- only if
    * the event is one of the event classes we care about, and we are
    * currently linked. Note that the event is assumed to originate from our
    * parent, so we don't apply it to ourselves.
    */
   public void pushChanges(DisplaySettingsEvent event) {
      if (!isActive_) {
         return;
      }
      boolean isRelevant = false;
      for (Class<?> eventClass : relevantEvents_) {
         if (eventClass.isInstance(event)) {
            isRelevant = true;
            break;
         }
      }
      if (!isRelevant) {
         return;
      }

      for (SettingsLinker linker : linkedLinkers_) {
         if (linker.getShouldApplyChanges(event)) {
            linker.applyChange(event);
         }
      }
   }

   /**
    * Return true iff the given DisplaySettingsEvent represents a change that
    * we need to apply to our own DisplayWindow.
    */
   public abstract boolean getShouldApplyChanges(DisplaySettingsEvent changeEvent);

   /**
    * Apply the change indicated by the provided DisplaySettingsEvent to
    * our own DisplayWindow.
    */
   public abstract void applyChange(DisplaySettingsEvent changeEvent);

   /**
    * Generate a semi-unique ID for this linker; it should indicate the
    * specific property, sub-property, or group of properties that this linker
    * handles synchronization for.
    */
   public abstract int getID();
}
